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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private DeliveryPartnerRepository deliveryPartnerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderEventRepository orderEventRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Clock restaurantClock;

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
        List<LineInput> sortedItems = validateAndSortItems(input, key);
        String requestFingerprint = requestHash(input, sortedItems);
        Optional<CustomerOrder> existingOrder =
                orderRepository.findByCustomerIdAndIdempotencyKey(actorId, key);
        if (existingOrder.isPresent()) {
            CustomerOrder previousOrder = existingOrder.get();
            if (!previousOrder.getRequestHash().equals(requestFingerprint)) {
                throw ApiException.conflict("Idempotency-Key already used for a different request");
            }

            return new Placement(response(previousOrder), true);
        }

        Restaurant restaurant =
                restaurantRepository
                        .findById(input.restaurantId())
                        .orElseThrow(() -> ApiException.notFound("Restaurant"));
        restaurant.requireAcceptingOrdersAt(LocalTime.now(restaurantClock));

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setDeliveryAddress(input.deliveryAddress().trim());
        order.setIdempotencyKey(key);
        order.setRequestHash(requestFingerprint);
        reserveItems(order, sortedItems);
        saveOrderWithPayment(order, input.paymentToken());
        notificationService.recordEvent(order, "PLACED", actorId);

        return new Placement(response(order), false);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public OrderResponse get(long actorId, long id) {
        CustomerOrder order =
                orderRepository.findById(id).orElseThrow(() -> ApiException.notFound("Order"));
        Access.requireOrderAccess(userService.get(actorId), order);

        return response(order);
    }

    @Override
    public PageResponse<OrderSummary> list(long actorId, OrderStatus status, int page, int size) {
        User user = userService.get(actorId);
        String statusName = status == null ? null : status.name();
        Pageable pageable = PageResponse.request(page, size);
        Page<CustomerOrder> result =
                orderRepository.findVisible(actorId, user.getRole().name(), statusName, pageable);

        return PageResponse.from(result.map(OrderSummary::from));
    }

    @Override
    @Transactional
    public OrderResponse transition(long actorId, long id, OrderStatus target) {
        User actor = userService.get(actorId);
        Access.requireRole(actor, target.requiredRole());
        CustomerOrder order = lockedOrder(id);
        Access.requireOrderAccess(actor, order);
        if (order.getStatus() == target) {
            return response(order);
        }

        order.getStatus().requireTransitionTo(target);
        if (target == OrderStatus.CANCELLED || target == OrderStatus.REJECTED) {
            refundOrder(order);
        }

        if (target == OrderStatus.DELIVERED) {
            DeliveryPartner partner =
                    deliveryPartnerRepository
                            .findLockedById(order.getPartner().getId())
                            .orElseThrow(() -> ApiException.notFound("Delivery partner"));
            partner.setActiveOrder(null);
        }

        order.setStatus(target);
        order.setUpdatedAt(Instant.now());
        notificationService.recordEvent(order, "STATUS_CHANGED", actorId);

        return response(order);
    }

    @Override
    @Transactional
    public OrderResponse claim(long actorId, long id) {
        Access.requireRole(userService.get(actorId), Role.PARTNER);
        // All assignment-changing flows lock order first, then partner, including delivery
        // completion.
        CustomerOrder order = lockedOrder(id);
        DeliveryPartner partner =
                deliveryPartnerRepository
                        .findLockedByUserId(actorId)
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
        notificationService.recordEvent(order, "PARTNER_ASSIGNED", actorId);

        return response(order);
    }

    private CustomerOrder lockedOrder(long id) {
        return orderRepository.findLockedById(id).orElseThrow(() -> ApiException.notFound("Order"));
    }

    private MenuItem lockedMenuItem(long id) {
        return menuItemRepository
                .findLockedById(id)
                .orElseThrow(() -> ApiException.notFound("Menu item"));
    }

    private List<LineInput> validateAndSortItems(PlaceOrder input, String key) {
        if (key == null || !key.matches("[a-zA-Z0-9._:-]{1,80}")) {
            throw ApiException.badRequest(
                    "Idempotency-Key must be 1..80 letters, digits, dots, underscores, colons or"
                            + " hyphens");
        }

        Set<Long> itemIds = new HashSet<>();
        for (LineInput item : input.items()) {
            if (!itemIds.add(item.menuItemId())) {
                throw ApiException.badRequest(
                        "Duplicate menu items are not allowed; use quantity instead");
            }
        }

        List<LineInput> sortedItems = new ArrayList<>(input.items());
        sortedItems.sort(Comparator.comparingLong(LineInput::menuItemId));

        return sortedItems;
    }

    private void reserveItems(CustomerOrder order, List<LineInput> sortedItems) {
        BigDecimal total = BigDecimal.ZERO;

        // Ascending item IDs keep inventory lock ordering consistent across baskets.
        for (LineInput requestedItem : sortedItems) {
            MenuItem item = lockedMenuItem(requestedItem.menuItemId());
            if (!item.getRestaurant().getId().equals(order.getRestaurant().getId())) {
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

            BigDecimal lineTotal =
                    item.getPrice().multiply(BigDecimal.valueOf(requestedItem.quantity()));
            total = total.add(lineTotal);
        }

        order.setTotal(total);
    }

    private void saveOrderWithPayment(CustomerOrder order, PaymentToken token) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotal());
        order.setPayment(payment);
        orderRepository.save(order);

        // A declined local payment rolls back the order and reserved stock in the same transaction.
        if (token == PaymentToken.TEST_DECLINE) {
            throw new ApiException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "PAYMENT_DECLINED",
                    "The simulated payment was declined");
        }
    }

    private void refundOrder(CustomerOrder order) {
        List<OrderItem> lines = new ArrayList<>(order.getItems());
        lines.sort(Comparator.comparing(line -> line.getMenuItem().getId()));

        for (OrderItem line : lines) {
            MenuItem item = lockedMenuItem(line.getMenuItem().getId());
            item.adjustStock(line.getQuantity());
        }

        order.getPayment().setStatus(Payment.Status.REFUNDED);
    }

    private OrderResponse response(CustomerOrder order) {
        return OrderResponse.from(
                order, orderEventRepository.findByOrderIdOrderByIdAsc(order.getId()));
    }

    private String requestHash(PlaceOrder input, List<LineInput> sortedItems) {
        try {
            PlaceOrder canonicalRequest =
                    new PlaceOrder(
                            input.restaurantId(),
                            sortedItems,
                            input.deliveryAddress().trim(),
                            input.paymentToken());

            byte[] requestBytes = objectMapper.writeValueAsBytes(canonicalRequest);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(requestBytes);

            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Cannot fingerprint order request", exception);
        }
    }
}
