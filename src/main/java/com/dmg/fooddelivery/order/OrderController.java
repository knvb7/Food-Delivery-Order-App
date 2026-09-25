package com.dmg.fooddelivery.order;

import static com.dmg.fooddelivery.order.OrderModels.*;
import com.dmg.fooddelivery.common.Page;
import com.dmg.fooddelivery.security.Accounts;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;
    private final Accounts accounts;
    public OrderController(OrderService orders, Accounts accounts) { this.orders = orders; this.accounts = accounts; }

    @PostMapping @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderView> place(@RequestHeader("Idempotency-Key") String key, @Valid @RequestBody PlaceOrder input) {
        var placed = orders.place(accounts.current(), input, key);
        return ResponseEntity.status(placed.replayed() ? 200 : 201)
                .location(URI.create("/api/orders/" + placed.order().id()))
                .header("Idempotency-Replayed", Boolean.toString(placed.replayed())).body(placed.order());
    }
    @GetMapping public Page<OrderSummary> list(@RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return orders.list(accounts.current(), status, page, size);
    }
    @GetMapping("/{id}") public OrderView get(@PathVariable long id) { return orders.get(accounts.current(), id); }
    @PatchMapping("/{id}/status") public OrderView status(@PathVariable long id, @Valid @RequestBody StatusUpdate input) {
        return orders.transition(accounts.current(), id, input.status());
    }
}
