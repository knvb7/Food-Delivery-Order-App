package com.dmg.fooddelivery.dto;

import com.dmg.fooddelivery.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() {}
    public enum PaymentToken { TEST_SUCCESS, TEST_DECLINE }
    public record LineInput(@Positive long menuItemId, @Min(1) @Max(100) int quantity) {}
    public record PlaceOrder(@Positive long restaurantId,
                             @NotEmpty @Size(max = 50) List<@NotNull @Valid LineInput> items,
                             @NotBlank @Size(max = 500) String deliveryAddress, @NotNull PaymentToken paymentToken) {}
    public record StatusUpdate(@NotNull OrderStatus status) {}
    public record LineResponse(long menuItemId, String name, BigDecimal unitPrice, int quantity) {}
    public record PaymentResponse(Payment.Status status, BigDecimal amount, String reference) {}
    public record EventResponse(long id, String type, OrderStatus status, long actorId, Instant createdAt) {
        public static EventResponse from(OrderEvent e) {
            return new EventResponse(e.getId(), e.getType(), e.getStatus(), e.getActorId(), e.getCreatedAt());
        }
    }
    public record OrderResponse(long id, long customerId, long restaurantId, Long partnerId, OrderStatus status,
                                 String deliveryAddress, BigDecimal total, String currency, List<LineResponse> items,
                                 PaymentResponse payment, List<EventResponse> history, Instant createdAt, Instant updatedAt) {
        public static OrderResponse from(CustomerOrder o, List<OrderEvent> events) {
            var lines = o.getItems().stream().map(i -> new LineResponse(i.getMenuItem().getId(), i.getName(), i.getUnitPrice(), i.getQuantity())).toList();
            var payment = o.getPayment();
            return new OrderResponse(o.getId(), o.getCustomer().getId(), o.getRestaurant().getId(),
                    o.getPartner() == null ? null : o.getPartner().getId(), o.getStatus(), o.getDeliveryAddress(), o.getTotal(),
                    "INR", lines, new PaymentResponse(payment.getStatus(), payment.getAmount(), payment.getReference()),
                    events.stream().map(EventResponse::from).toList(), o.getCreatedAt(), o.getUpdatedAt());
        }
    }
    public record OrderSummary(long id, long restaurantId, Long partnerId, OrderStatus status, BigDecimal total, Instant createdAt) {
        public static OrderSummary from(CustomerOrder o) {
            return new OrderSummary(o.getId(), o.getRestaurant().getId(), o.getPartner() == null ? null : o.getPartner().getId(),
                    o.getStatus(), o.getTotal(), o.getCreatedAt());
        }
    }
    public record Placement(OrderResponse order, boolean replayed) {}
}
