package com.dmg.fooddelivery.delivery;

import static com.dmg.fooddelivery.delivery.DeliveryModels.*;
import com.dmg.fooddelivery.common.Page;
import com.dmg.fooddelivery.order.OrderModels.OrderView;
import com.dmg.fooddelivery.order.OrderService;
import com.dmg.fooddelivery.security.Accounts;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DeliveryController {
    private final DeliveryRepository partners;
    private final DeliveryService service;
    private final OrderService orders;
    private final Accounts accounts;
    public DeliveryController(DeliveryRepository partners, DeliveryService service, OrderService orders, Accounts accounts) {
        this.partners = partners; this.service = service; this.orders = orders; this.accounts = accounts;
    }
    @PostMapping("/admin/partners") @ResponseStatus(HttpStatus.CREATED)
    public Partner create(@Valid @RequestBody PartnerInput input) { return service.create(accounts.current(), input); }
    @PutMapping("/admin/partners/{id}") public Partner update(@PathVariable long id, @Valid @RequestBody PartnerUpdate input) {
        return service.update(accounts.current(), id, input);
    }
    @GetMapping("/admin/partners") public Page<Partner> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return partners.list(page, size);
    }
    @GetMapping("/delivery/me") @PreAuthorize("hasRole('PARTNER')")
    public Partner me() { return partners.byUser(accounts.current().id(), false); }
    @GetMapping("/delivery/orders/available") @PreAuthorize("hasRole('PARTNER')")
    public Page<AvailableOrder> available(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.available(accounts.current(), page, size);
    }
    @PostMapping("/orders/{id}/claim") @PreAuthorize("hasRole('PARTNER')")
    public OrderView claim(@PathVariable long id) { return orders.claim(accounts.current(), id); }
}
