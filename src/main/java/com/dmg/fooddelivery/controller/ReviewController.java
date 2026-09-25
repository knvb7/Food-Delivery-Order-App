package com.dmg.fooddelivery.controller;

import com.dmg.fooddelivery.dto.ReviewDtos.*;
import com.dmg.fooddelivery.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviews;
    @PostMapping("/orders/{id}/review") @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@RequestHeader("X-User-Id") long actorId, @PathVariable long id, @Valid @RequestBody ReviewInput input) {
        return reviews.create(actorId, id, input);
    }
    @GetMapping("/restaurants/{id}/reviews") public RestaurantReviews list(@PathVariable long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return reviews.list(id, page, size);
    }
}
