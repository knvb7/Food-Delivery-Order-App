package com.dmg.fooddelivery.controller;

import com.dmg.fooddelivery.dto.OrderDtos.*;
import com.dmg.fooddelivery.dto.PageResponse;
import com.dmg.fooddelivery.model.OrderStatus;
import com.dmg.fooddelivery.service.OrderService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/orders") @RequiredArgsConstructor
public class OrderController {
    private final OrderService orders;
    @PostMapping public ResponseEntity<OrderResponse> place(@RequestHeader("X-User-Id") long actorId,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody PlaceOrder input) {
        var placed = orders.place(actorId, input, key);
        return ResponseEntity.status(placed.replayed() ? 200 : 201).location(URI.create("/api/orders/" + placed.order().id()))
                .header("Idempotency-Replayed", Boolean.toString(placed.replayed())).body(placed.order());
    }
    @GetMapping public PageResponse<OrderSummary> list(@RequestHeader("X-User-Id") long actorId, @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return orders.list(actorId, status, page, size);
    }
    @GetMapping("/{id}") public OrderResponse get(@RequestHeader("X-User-Id") long actorId, @PathVariable long id) {
        return orders.get(actorId, id);
    }
    @PatchMapping("/{id}/status") public OrderResponse status(@RequestHeader("X-User-Id") long actorId, @PathVariable long id,
            @Valid @RequestBody StatusUpdate input) {
        return orders.transition(actorId, id, input.status());
    }
    @PostMapping("/{id}/claim") public OrderResponse claim(@RequestHeader("X-User-Id") long actorId, @PathVariable long id) {
        return orders.claim(actorId, id);
    }
}
