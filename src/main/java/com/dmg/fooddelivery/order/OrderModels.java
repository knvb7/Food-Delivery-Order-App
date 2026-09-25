package com.dmg.fooddelivery.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderModels {
    private OrderModels() {}
    public enum PaymentToken { TEST_SUCCESS, TEST_DECLINE }
    public record LineInput(@Positive long menuItemId, @Min(1) @Max(100) int quantity) {}
    public record PlaceOrder(@Positive long restaurantId,
                             @NotEmpty @Size(max = 50) List<@NotNull @Valid LineInput> items,
                             @NotBlank @Size(max = 500) String deliveryAddress,
                             @NotNull PaymentToken paymentToken) {}
    public record StatusUpdate(@NotNull OrderStatus status) {}
    public record OrderRow(long id, long customerId, long restaurantId, Long partnerId,
                            OrderStatus status, String deliveryAddress, BigDecimal total,
                            String requestHash, Instant createdAt, Instant updatedAt) {}
    public record Line(long menuItemId, String name, BigDecimal unitPrice, int quantity) {}
    public record Payment(String status, BigDecimal amount, String reference) {}
    public record Event(long id, String type, OrderStatus status, long actorId, Instant createdAt) {}
    public record OrderView(long id, long customerId, long restaurantId, Long partnerId, OrderStatus status,
                             String deliveryAddress, BigDecimal total, String currency, List<Line> items,
                             Payment payment, List<Event> history, Instant createdAt, Instant updatedAt) {}
    public record OrderSummary(long id, long restaurantId, Long partnerId, OrderStatus status,
                                BigDecimal total, Instant createdAt) {}
    public record Placement(OrderView order, boolean replayed) {}
}
