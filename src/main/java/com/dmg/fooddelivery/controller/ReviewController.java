package com.dmg.fooddelivery.controller;

import com.dmg.fooddelivery.dto.ReviewDtos.RestaurantReviews;
import com.dmg.fooddelivery.dto.ReviewDtos.ReviewInput;
import com.dmg.fooddelivery.dto.ReviewDtos.ReviewResponse;
import com.dmg.fooddelivery.service.ReviewService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviews;

    @PostMapping("/orders/{id}/review")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(
            @RequestHeader("X-User-Id") long actorId,
            @PathVariable long id,
            @Valid @RequestBody ReviewInput input) {
        return reviews.create(actorId, id, input);
    }

    @GetMapping("/restaurants/{id}/reviews")
    public RestaurantReviews list(
            @PathVariable long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reviews.list(id, page, size);
    }
}
