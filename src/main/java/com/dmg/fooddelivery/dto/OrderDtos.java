package com.dmg.fooddelivery.dto;

import com.dmg.fooddelivery.model.CustomerOrder;
import com.dmg.fooddelivery.model.OrderEvent;
import com.dmg.fooddelivery.model.OrderItem;
import com.dmg.fooddelivery.model.OrderStatus;
import com.dmg.fooddelivery.model.Payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {}

    public enum PaymentToken {
        TEST_SUCCESS,
        TEST_DECLINE
    }

    public record LineInput(@Positive long menuItemId, @Min(1) @Max(100) int quantity) {}

    public record PlaceOrder(
            @Positive long restaurantId,
            @NotEmpty @Size(max = 50) List<@NotNull @Valid LineInput> items,
            @NotBlank @Size(max = 500) String deliveryAddress,
            @NotNull PaymentToken paymentToken) {}

    public record StatusUpdate(@NotNull OrderStatus status) {}

    public record LineResponse(long menuItemId, String name, BigDecimal unitPrice, int quantity) {
        public static LineResponse from(OrderItem item) {
            return new LineResponse(
                    item.getMenuItem().getId(),
                    item.getName(),
                    item.getUnitPrice(),
                    item.getQuantity());
        }
    }

    public record PaymentResponse(Payment.Status status, BigDecimal amount, String reference) {
        public static PaymentResponse from(Payment payment) {
            return new PaymentResponse(
                    payment.getStatus(), payment.getAmount(), payment.getReference());
        }
    }

    public record EventResponse(
            long id, String type, OrderStatus status, long actorId, Instant createdAt) {
        public static EventResponse from(OrderEvent event) {
            return new EventResponse(
                    event.getId(),
                    event.getType(),
                    event.getStatus(),
                    event.getActorId(),
                    event.getCreatedAt());
        }
    }

    public record OrderResponse(
            long id,
            long customerId,
            long restaurantId,
            Long partnerId,
            OrderStatus status,
            String deliveryAddress,
            BigDecimal total,
            String currency,
            List<LineResponse> items,
            PaymentResponse payment,
            List<EventResponse> history,
            Instant createdAt,
            Instant updatedAt) {
        public static OrderResponse from(CustomerOrder order, List<OrderEvent> events) {
            List<LineResponse> lines = order.getItems().stream().map(LineResponse::from).toList();
            List<EventResponse> history = events.stream().map(EventResponse::from).toList();
            PaymentResponse payment = PaymentResponse.from(order.getPayment());
            Long partnerId = order.getPartner() == null ? null : order.getPartner().getId();

            return new OrderResponse(
                    order.getId(),
                    order.getCustomer().getId(),
                    order.getRestaurant().getId(),
                    partnerId,
                    order.getStatus(),
                    order.getDeliveryAddress(),
                    order.getTotal(),
                    "INR",
                    lines,
                    payment,
                    history,
                    order.getCreatedAt(),
                    order.getUpdatedAt());
        }
    }

    public record OrderSummary(
            long id,
            long restaurantId,
            Long partnerId,
            OrderStatus status,
            BigDecimal total,
            Instant createdAt) {
        public static OrderSummary from(CustomerOrder order) {
            return new OrderSummary(
                    order.getId(),
                    order.getRestaurant().getId(),
                    order.getPartner() == null ? null : order.getPartner().getId(),
                    order.getStatus(),
                    order.getTotal(),
                    order.getCreatedAt());
        }
    }

    public record Placement(OrderResponse order, boolean replayed) {}
}
