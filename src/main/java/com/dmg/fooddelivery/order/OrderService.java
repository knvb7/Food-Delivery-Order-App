package com.dmg.fooddelivery.order;

import static com.dmg.fooddelivery.order.OrderModels.*;
import com.dmg.fooddelivery.catalog.CatalogRepository;
import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.common.Page;
import com.dmg.fooddelivery.delivery.DeliveryRepository;
import com.dmg.fooddelivery.notification.OrderEvents;
import com.dmg.fooddelivery.security.Accounts;
import com.dmg.fooddelivery.security.Actor;
import com.dmg.fooddelivery.security.Role;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final CatalogRepository catalog;
    private final DeliveryRepository partners;
    private final Accounts accounts;
    private final PaymentService payments;
    private final OrderEvents events;
    private final ObjectMapper mapper;
    public OrderService(OrderRepository orders, CatalogRepository catalog, DeliveryRepository partners,
                        Accounts accounts, PaymentService payments, OrderEvents events, ObjectMapper mapper) {
        this.orders = orders; this.catalog = catalog; this.partners = partners; this.accounts = accounts;
        this.payments = payments; this.events = events; this.mapper = mapper;
    }

    @Transactional
    public Placement place(Actor actor, PlaceOrder input, String key) {
        actor.require(Role.CUSTOMER);
        if (key == null || !key.matches("[a-zA-Z0-9._:-]{1,80}")) {
            throw ApiException.badRequest("Idempotency-Key must be 1..80 letters, digits, dots, underscores, colons or hyphens");
        }
        var sorted = input.items().stream().sorted(Comparator.comparingLong(LineInput::menuItemId)).toList();
        if (sorted.stream().map(LineInput::menuItemId).distinct().count() != sorted.size()) {
            throw ApiException.badRequest("Duplicate menu items are not allowed; use quantity instead");
        }
        String hash = requestHash(input, sorted);
        accounts.lockCustomer(actor.id());
        var previous = orders.byKey(actor.id(), key);
        if (previous.isPresent()) {
            if (!previous.get().requestHash().equals(hash)) throw ApiException.conflict("Idempotency-Key already used for a different request");
            return new Placement(orders.view(previous.get()), true);
        }
        var restaurant = catalog.restaurant(input.restaurantId());
        if (!restaurant.active() || !catalog.city(restaurant.cityId()).active()) {
            throw ApiException.conflict("Restaurant or city is not accepting new orders");
        }
        var lines = new ArrayList<Line>();
        BigDecimal total = BigDecimal.ZERO;
        // Ascending item IDs give overlapping baskets the same lock order.
        for (var requested : sorted) {
            var item = catalog.menuItem(requested.menuItemId(), true);
            if (item.restaurantId() != restaurant.id()) throw ApiException.badRequest("All items must belong to the selected restaurant");
            if (!item.available()) throw ApiException.conflict("Menu item " + item.id() + " is unavailable");
            if (item.stock() < requested.quantity()) throw ApiException.conflict("Insufficient stock for menu item " + item.id());
            catalog.adjustStock(item.id(), -requested.quantity());
            lines.add(new Line(item.id(), item.name(), item.price(), requested.quantity()));
            total = total.add(item.price().multiply(BigDecimal.valueOf(requested.quantity())));
        }
        long id = orders.create(actor.id(), input, key, hash, total);
        lines.forEach(line -> orders.addLine(id, line));
        payments.capture(id, total, input.paymentToken());
        var order = orders.get(id, false);
        events.append(order, "PLACED", actor.id());
        return new Placement(orders.view(order), false);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public OrderView get(Actor actor, long id) {
        var order = orders.get(id, false);
        requireAccess(actor, order);
        return orders.view(order);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<OrderSummary> list(Actor actor, OrderStatus status, int page, int size) {
        return orders.list(actor, status, page, size);
    }

    @Transactional
    public OrderView transition(Actor actor, long id, OrderStatus target) {
        OrderPolicy.requireRole(actor.role(), target);
        var order = orders.get(id, true);
        requireAccess(actor, order);
        // Authorized retries have no additional stock, payment, or notification effects.
        if (order.status() == target) return orders.view(order);
        OrderPolicy.requireTransition(order.status(), target);
        if (target == OrderStatus.OUT_FOR_DELIVERY && order.partnerId() == null) {
            throw ApiException.conflict("Assign a delivery partner before pickup");
        }
        if (target == OrderStatus.CANCELLED || target == OrderStatus.REJECTED) {
            for (var line : orders.lines(id)) { // Repository returns ascending item IDs.
                catalog.menuItem(line.menuItemId(), true);
                catalog.adjustStock(line.menuItemId(), line.quantity());
            }
            payments.refund(id);
        }
        if (target == OrderStatus.DELIVERED) {
            partners.get(order.partnerId(), true);
            partners.release(order.partnerId(), id);
        }
        orders.setStatus(id, target);
        var updated = orders.get(id, false);
        events.append(updated, "STATUS_CHANGED", actor.id());
        return orders.view(updated);
    }

    @Transactional
    public OrderView claim(Actor actor, long id) {
        actor.require(Role.PARTNER);
        // All assignment-changing flows lock order first, then partner. No reverse lock acquisition.
        var order = orders.get(id, true);
        var partner = partners.byUser(actor.id(), true);
        if (order.partnerId() != null) {
            if (order.partnerId() == partner.id()) return orders.view(order);
            throw ApiException.conflict("Order already assigned to another partner");
        }
        if (!OrderPolicy.assignable(order.status())) throw ApiException.conflict("Only accepted or preparing orders can be claimed");
        if (!partner.active()) throw ApiException.conflict("Partner is inactive");
        if (partner.cityId() != catalog.restaurant(order.restaurantId()).cityId()) throw ApiException.conflict("Partner must operate in the restaurant's city");
        if (partner.activeOrderId() != null) throw ApiException.conflict("Partner already has an active order");
        partners.occupy(partner.id(), id);
        orders.assign(id, partner.id());
        var updated = orders.get(id, false);
        events.append(updated, "PARTNER_ASSIGNED", actor.id());
        return orders.view(updated);
    }

    private void requireAccess(Actor actor, OrderRow order) {
        boolean allowed = switch (actor.role()) {
            case ADMIN -> true;
            case CUSTOMER -> actor.id() == order.customerId();
            case OWNER -> actor.id() == catalog.restaurant(order.restaurantId()).ownerId();
            case PARTNER -> order.partnerId() != null && partners.get(order.partnerId(), false).userId() == actor.id();
        };
        if (!allowed) throw ApiException.forbidden();
    }

    private String requestHash(PlaceOrder input, List<LineInput> sorted) {
        try {
            var canonical = new PlaceOrder(input.restaurantId(), sorted, input.deliveryAddress().trim(), input.paymentToken());
            byte[] bytes = mapper.writeValueAsString(canonical).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Cannot fingerprint order request", ex);
        }
    }
}
