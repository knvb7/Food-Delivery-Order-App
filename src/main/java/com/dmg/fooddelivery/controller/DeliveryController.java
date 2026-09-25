package com.dmg.fooddelivery.controller;

import com.dmg.fooddelivery.dto.DeliveryDtos.*;
import com.dmg.fooddelivery.dto.PageResponse;
import com.dmg.fooddelivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class DeliveryController {
    private final DeliveryService delivery;
    @PostMapping("/admin/partners") @ResponseStatus(HttpStatus.CREATED)
    public PartnerResponse create(@RequestHeader("X-User-Id") long actorId, @Valid @RequestBody PartnerInput input) {
        return delivery.create(actorId, input);
    }
    @PutMapping("/admin/partners/{id}") public PartnerResponse update(@RequestHeader("X-User-Id") long actorId,
            @PathVariable long id, @Valid @RequestBody PartnerUpdate input) {
        return delivery.update(actorId, id, input);
    }
    @GetMapping("/admin/partners") public PageResponse<PartnerResponse> list(@RequestHeader("X-User-Id") long actorId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return delivery.list(actorId, page, size);
    }
    @GetMapping("/delivery/me") public PartnerResponse me(@RequestHeader("X-User-Id") long actorId) { return delivery.me(actorId); }
    @GetMapping("/delivery/orders/available") public PageResponse<AvailableOrder> available(@RequestHeader("X-User-Id") long actorId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return delivery.available(actorId, page, size);
    }
}
