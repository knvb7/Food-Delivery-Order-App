package com.dmg.fooddelivery.service.impl;

import com.dmg.fooddelivery.dto.PageResponse;
import com.dmg.fooddelivery.model.CustomerOrder;
import com.dmg.fooddelivery.model.Notification;
import com.dmg.fooddelivery.model.OrderEvent;
import com.dmg.fooddelivery.repository.NotificationRepository;
import com.dmg.fooddelivery.repository.OrderEventRepository;
import com.dmg.fooddelivery.service.NotificationService;
import com.dmg.fooddelivery.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private OrderEventRepository orderEventRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserService userService;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordEvent(CustomerOrder order, String type, long actorId) {
        OrderEvent event = new OrderEvent();
        event.setOrder(order);
        event.setType(type);
        event.setStatus(order.getStatus());
        event.setActorId(actorId);
        event.setCustomerId(order.getCustomer().getId());
        event.setOwnerId(order.getRestaurant().getOwner().getId());
        if (order.getPartner() != null) {
            event.setPartnerUserId(order.getPartner().getUser().getId());
        }

        orderEventRepository.save(event);
    }

    @Override
    public List<Long> pendingEvents() {
        return orderEventRepository.findPendingIds(PageRequest.of(0, 100));
    }

    @Override
    @Async("notificationTaskExecutor")
    @Transactional
    public void dispatch(long eventId) {
        OrderEvent event = orderEventRepository.findLockedById(eventId).orElse(null);
        if (event == null || event.isDispatched()) {
            return;
        }

        Set<Long> recipients = new LinkedHashSet<>();
        recipients.add(event.getCustomerId());
        recipients.add(event.getOwnerId());
        if (event.getPartnerUserId() != null) {
            recipients.add(event.getPartnerUserId());
        }

        for (long recipientId : recipients) {
            NotificationContent content = contentFor(event, recipientId);
            Notification notification = new Notification();
            notification.setEvent(event);
            notification.setRecipientId(recipientId);
            notification.setTitle(content.title());
            notification.setMessage(content.message());
            notificationRepository.save(notification);
        }

        // Both inbox delivery and acknowledgement commit together; a failure leaves the event
        // retryable.
        event.setDispatched(true);
    }

    @Override
    public PageResponse<NotificationResponse> inbox(long actorId, int page, int size) {
        userService.get(actorId);
        Pageable pageable = PageResponse.request(page, size);
        Page<Notification> result = notificationRepository.findByRecipientId(actorId, pageable);

        return PageResponse.from(result.map(this::toResponse));
    }

    private NotificationResponse toResponse(Notification notification) {
        OrderEvent event = notification.getEvent();
        NotificationContent fallback = contentFor(event, notification.getRecipientId());
        String title = notification.getTitle() == null ? fallback.title() : notification.getTitle();
        String message =
                notification.getMessage() == null ? fallback.message() : notification.getMessage();

        return new NotificationResponse(
                notification.getId(),
                event.getOrder().getId(),
                event.getType(),
                event.getStatus(),
                title,
                message,
                notification.getCreatedAt());
    }

    private NotificationContent contentFor(OrderEvent event, long recipientId) {
        if (recipientId == event.getCustomerId()) {
            return customerContent(event);
        }
        if (recipientId == event.getOwnerId()) {
            return ownerContent(event);
        }

        return partnerContent(event);
    }

    private NotificationContent customerContent(OrderEvent event) {
        long orderId = event.getOrder().getId();
        String restaurantName = event.getOrder().getRestaurant().getName();
        if ("PARTNER_ASSIGNED".equals(event.getType())) {
            String partnerName = event.getOrder().getPartner().getUser().getFullName();
            return new NotificationContent(
                    "Delivery partner assigned",
                    partnerName + " has been assigned to deliver your order #" + orderId + ".");
        }

        return switch (event.getStatus()) {
            case PLACED ->
                    new NotificationContent(
                            "Order placed",
                            "Your order #" + orderId + " was placed with " + restaurantName + ".");
            case ACCEPTED ->
                    new NotificationContent(
                            "Order accepted",
                            restaurantName + " accepted your order #" + orderId + ".");
            case PREPARING ->
                    new NotificationContent(
                            "Order is being prepared",
                            restaurantName + " is preparing your order #" + orderId + ".");
            case OUT_FOR_DELIVERY ->
                    new NotificationContent(
                            "Order is on the way",
                            "Your order #" + orderId + " is out for delivery.");
            case DELIVERED ->
                    new NotificationContent(
                            "Order delivered",
                            "Your order #" + orderId + " was delivered. You can now review it.");
            case REJECTED ->
                    new NotificationContent(
                            "Order rejected",
                            restaurantName
                                    + " rejected order #"
                                    + orderId
                                    + ". The payment was refunded.");
            case CANCELLED ->
                    new NotificationContent(
                            "Order cancelled",
                            "Your order #" + orderId + " was cancelled and the payment was refunded.");
        };
    }

    private NotificationContent ownerContent(OrderEvent event) {
        long orderId = event.getOrder().getId();
        if ("PLACED".equals(event.getType())) {
            return new NotificationContent(
                    "New order received", "A customer placed order #" + orderId + ".");
        }
        if ("PARTNER_ASSIGNED".equals(event.getType())) {
            String partnerName = event.getOrder().getPartner().getUser().getFullName();
            return new NotificationContent(
                    "Delivery partner assigned",
                    partnerName + " was assigned to order #" + orderId + ".");
        }

        return switch (event.getStatus()) {
            case PLACED ->
                    new NotificationContent(
                            "New order received", "A customer placed order #" + orderId + ".");
            case ACCEPTED ->
                    new NotificationContent(
                            "Order accepted", "Order #" + orderId + " was accepted.");
            case PREPARING ->
                    new NotificationContent(
                            "Order preparation started",
                            "Order #" + orderId + " is now being prepared.");
            case OUT_FOR_DELIVERY ->
                    new NotificationContent(
                            "Order picked up",
                            "Order #" + orderId + " is now out for delivery.");
            case DELIVERED ->
                    new NotificationContent(
                            "Order completed", "Order #" + orderId + " was delivered.");
            case REJECTED ->
                    new NotificationContent(
                            "Order rejected",
                            "Order #" + orderId + " was rejected and its stock was restored.");
            case CANCELLED ->
                    new NotificationContent(
                            "Order cancelled by customer",
                            "The customer cancelled order #"
                                    + orderId
                                    + "; its stock was restored.");
        };
    }

    private NotificationContent partnerContent(OrderEvent event) {
        long orderId = event.getOrder().getId();
        if ("PARTNER_ASSIGNED".equals(event.getType())) {
            return new NotificationContent(
                    "New delivery assigned",
                    "You have been assigned order #"
                            + orderId
                            + " from "
                            + event.getOrder().getRestaurant().getName()
                            + ".");
        }

        return switch (event.getStatus()) {
            case PREPARING ->
                    new NotificationContent(
                            "Order is being prepared",
                            "Order #" + orderId + " is being prepared for pickup.");
            case OUT_FOR_DELIVERY ->
                    new NotificationContent(
                            "Delivery started", "You are delivering order #" + orderId + ".");
            case DELIVERED ->
                    new NotificationContent(
                            "Delivery completed",
                            "Order #" + orderId + " was delivered successfully.");
            default ->
                    new NotificationContent(
                            "Order update",
                            "Order #" + orderId + " is now " + event.getStatus() + ".");
        };
    }

    private record NotificationContent(String title, String message) {}
}
