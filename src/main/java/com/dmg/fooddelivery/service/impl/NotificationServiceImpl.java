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
            Notification notification = new Notification();
            notification.setEvent(event);
            notification.setRecipientId(recipientId);
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

        return new NotificationResponse(
                notification.getId(),
                event.getOrder().getId(),
                event.getType(),
                event.getStatus(),
                notification.getCreatedAt());
    }
}
