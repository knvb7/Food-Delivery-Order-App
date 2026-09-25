package com.dmg.fooddelivery.review;

import static com.dmg.fooddelivery.review.ReviewService.*;
import com.dmg.fooddelivery.security.Accounts;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ReviewController {
    private final ReviewService reviews;
    private final Accounts accounts;
    public ReviewController(ReviewService reviews, Accounts accounts) { this.reviews = reviews; this.accounts = accounts; }
    @PostMapping("/orders/{id}/review") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('CUSTOMER')")
    public Review create(@PathVariable long id, @Valid @RequestBody ReviewInput input) {
        return reviews.create(accounts.current(), id, input);
    }
    @GetMapping("/restaurants/{id}/reviews") public Reviews list(@PathVariable long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return reviews.list(id, page, size);
    }
}
