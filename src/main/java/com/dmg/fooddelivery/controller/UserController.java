package com.dmg.fooddelivery.controller;

import com.dmg.fooddelivery.dto.UserDtos.*;
import com.dmg.fooddelivery.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class UserController {
    private final UserService users;
    @PostMapping("/customers") @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody Registration input) { return users.register(input); }

    @PostMapping("/admin/users") @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@RequestHeader("X-User-Id") long actorId, @Valid @RequestBody CreateUser input) {
        return users.create(actorId, input);
    }
    @GetMapping("/me") public UserResponse me(@RequestHeader("X-User-Id") long actorId) {
        return UserResponse.from(users.get(actorId));
    }
}
