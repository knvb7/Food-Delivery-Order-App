package com.dmg.fooddelivery.service.impl;

import com.dmg.fooddelivery.common.Access;
import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.dto.OrderDtos.LineInput;
import com.dmg.fooddelivery.dto.OrderDtos.OrderResponse;
import com.dmg.fooddelivery.dto.OrderDtos.OrderSummary;
import com.dmg.fooddelivery.dto.OrderDtos.PaymentToken;
import com.dmg.fooddelivery.dto.OrderDtos.PlaceOrder;
import com.dmg.fooddelivery.dto.OrderDtos.Placement;
import com.dmg.fooddelivery.dto.PageResponse;
import com.dmg.fooddelivery.model.CustomerOrder;
import com.dmg.fooddelivery.model.DeliveryPartner;
import com.dmg.fooddelivery.model.MenuItem;
import com.dmg.fooddelivery.model.OrderItem;
import com.dmg.fooddelivery.model.OrderStatus;
import com.dmg.fooddelivery.model.Payment;
import com.dmg.fooddelivery.model.Restaurant;
import com.dmg.fooddelivery.model.Role;
import com.dmg.fooddelivery.model.User;
import com.dmg.fooddelivery.repository.DeliveryPartnerRepository;
import com.dmg.fooddelivery.repository.MenuItemRepository;
import com.dmg.fooddelivery.repository.OrderEventRepository;
import com.dmg.fooddelivery.repository.OrderRepository;
import com.dmg.fooddelivery.repository.RestaurantRepository;
import com.dmg.fooddelivery.repository.UserRepository;
import com.dmg.fooddelivery.service.NotificationService;
import com.dmg.fooddelivery.service.OrderService;
import com.dmg.fooddelivery.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orders;
    private final RestaurantRepository restaurants;
    private final MenuItemRepository menuItems;
    private final DeliveryPartnerRepository partners;
    private final UserRepository userRepository;
    private final OrderEventRepository events;
    private final UserService users;
    private final NotificationService notifications;
    private final ObjectMapper mapper;

    @Override
    @Transactional
    public Placement place(long actorId, PlaceOrder input, String key) {
        // Serialize this customer's requests so two first-time uses of the same key cannot both
        // reserve stock.
        User customer =
                userRepository
                        .findLockedById(actorId)
                        .orElseThrow(() -> ApiException.notFound("User"));
        Access.requireRole(customer, Role.CUSTOMER);
        if (key == null || !key.matches("[a-zA-Z0-9._:-]{1,80}")) {
            throw ApiException.badRequest(
                    "Idempotency-Key must be 1..80 letters, digits, dots, underscores, colons or"
                            + " hyphens");
        }

        List<LineInput> sortedItems =
                input.items().stream()
                        .sorted(Comparator.comparingLong(LineInput::menuItemId))
                        .toList();
        if (sortedItems.stream().map(LineInput::menuItemId).distinct().count()
                != sortedItems.size()) {
            throw ApiException.badRequest(
                    "Duplicate menu items are not allowed; use quantity instead");
        }

        String requestFingerprint = requestHash(input, sortedItems);
        Optional<CustomerOrder> existingOrder =
                orders.findByCustomerIdAndIdempotencyKey(actorId, key);
        if (existingOrder.isPresent()) {
            if (!existingOrder.get().getRequestHash().equals(requestFingerprint)) {
                throw ApiException.conflict("Idempotency-Key already used for a different request");
            }

            return new Placement(response(existingOrder.get()), true);
        }

        Restaurant restaurant =
                restaurants
                        .findById(input.restaurantId())
                        .orElseThrow(() -> ApiException.notFound("Restaurant"));
        if (!restaurant.isActive() || !restaurant.getCity().isActive()) {
            throw ApiException.conflict("Restaurant or city is not accepting new orders");
        }

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setDeliveryAddress(input.deliveryAddress().trim());
        order.setIdempotencyKey(key);
        order.setRequestHash(requestFingerprint);
        BigDecimal total = BigDecimal.ZERO;

        // Every inventory-changing flow locks item rows in ascending ID order to avoid basket
        // deadlocks.
        for (LineInput requestedItem : sortedItems) {
            MenuItem item =
                    menuItems
                            .findLockedById(requestedItem.menuItemId())
                            .orElseThrow(() -> ApiException.notFound("Menu item"));
            if (!item.getRestaurant().getId().equals(restaurant.getId())) {
                throw ApiException.badRequest("All items must belong to the selected restaurant");
            }

            if (!item.isAvailable()) {
                throw ApiException.conflict("Menu item " + item.getId() + " is unavailable");
            }

            item.adjustStock(-requestedItem.quantity());

            OrderItem line = new OrderItem();
            line.setOrder(order);
            line.setMenuItem(item);
            line.setName(item.getName());
            line.setUnitPrice(item.getPrice());
            line.setQuantity(requestedItem.quantity());
            order.getItems().add(line);
            total =
                    total.add(
                            item.getPrice().multiply(BigDecimal.valueOf(requestedItem.quantity())));
        }

        order.setTotal(total);
        // This local payment ledger participates in the order transaction. No external gateway is
        // called.

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(total);
        order.setPayment(payment);
        orders.save(order);
        if (input.paymentToken() == PaymentToken.TEST_DECLINE) {
            throw new ApiException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "PAYMENT_DECLINED",
                    "The simulated payment was declined");
        }

        notifications.recordEvent(order, "PLACED", actorId);

        return new Placement(response(order), false);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public OrderResponse get(long actorId, long id) {
        CustomerOrder order = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order"));
        Access.requireOrderAccess(users.get(actorId), order);

        return response(order);
    }

    @Override
    public PageResponse<OrderSummary> list(long actorId, OrderStatus status, int page, int size) {
        User user = users.get(actorId);
        String statusName = status == null ? null : status.name();

        return PageResponse.from(
                orders.findVisible(
                                actorId,
                                user.getRole().name(),
                                statusName,
                                PageResponse.request(page, size))
                        .map(OrderSummary::from));
    }

    @Override
    @Transactional
    public OrderResponse transition(long actorId, long id, OrderStatus target) {
        User actor = users.get(actorId);
        Access.requireRole(actor, target.requiredRole());
        CustomerOrder order = lockedOrder(id);
        Access.requireOrderAccess(actor, order);
        if (order.getStatus() == target) {
            return response(order);
        }

        order.getStatus().requireTransitionTo(target);
        if (target == OrderStatus.CANCELLED || target == OrderStatus.REJECTED) {
            List<OrderItem> lines =
                    order.getItems().stream()
                            .sorted(Comparator.comparing(line -> line.getMenuItem().getId()))
                            .toList();
            for (OrderItem line : lines) {
                MenuItem item =
                        menuItems
                                .findLockedById(line.getMenuItem().getId())
                                .orElseThrow(() -> ApiException.notFound("Menu item"));
                item.adjustStock(line.getQuantity());
            }

            order.getPayment().setStatus(Payment.Status.REFUNDED);
        }

        if (target == OrderStatus.DELIVERED) {
            DeliveryPartner partner =
                    partners.findLockedById(order.getPartner().getId())
                            .orElseThrow(() -> ApiException.notFound("Delivery partner"));
            partner.setActiveOrder(null);
        }

        order.setStatus(target);
        order.setUpdatedAt(Instant.now());
        notifications.recordEvent(order, "STATUS_CHANGED", actorId);

        return response(order);
    }

    @Override
    @Transactional
    public OrderResponse claim(long actorId, long id) {
        Access.requireRole(users.get(actorId), Role.PARTNER);
        // All assignment-changing flows lock order first, then partner, including delivery
        // completion.
        CustomerOrder order = lockedOrder(id);
        DeliveryPartner partner =
                partners.findLockedByUserId(actorId)
                        .orElseThrow(() -> ApiException.notFound("Delivery partner profile"));
        if (order.getPartner() != null) {
            if (order.getPartner().getId().equals(partner.getId())) {
                return response(order);
            }

            throw ApiException.conflict("Order already assigned to another partner");
        }

        if (!order.getStatus().isAssignable()) {
            throw ApiException.conflict("Only accepted or preparing orders can be claimed");
        }

        if (!partner.isActive()) {
            throw ApiException.conflict("Partner is inactive");
        }

        if (!partner.getCity().getId().equals(order.getRestaurant().getCity().getId())) {
            throw ApiException.conflict("Partner must operate in the restaurant's city");
        }

        if (partner.getActiveOrder() != null) {
            throw ApiException.conflict("Partner already has an active order");
        }

        partner.setActiveOrder(order);
        order.setPartner(partner);
        order.setUpdatedAt(Instant.now());
        notifications.recordEvent(order, "PARTNER_ASSIGNED", actorId);

        return response(order);
    }

    private CustomerOrder lockedOrder(long id) {
        return orders.findLockedById(id).orElseThrow(() -> ApiException.notFound("Order"));
    }

    private OrderResponse response(CustomerOrder order) {
        return OrderResponse.from(order, events.findByOrderIdOrderByIdAsc(order.getId()));
    }

    private String requestHash(PlaceOrder input, List<LineInput> sortedItems) {
        try {
            PlaceOrder canonicalRequest =
                    new PlaceOrder(
                            input.restaurantId(),
                            sortedItems,
                            input.deliveryAddress().trim(),
                            input.paymentToken());

            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(mapper.writeValueAsBytes(canonicalRequest)));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Cannot fingerprint order request", exception);
        }
    }
}
