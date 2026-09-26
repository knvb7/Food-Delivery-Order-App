package com.dmg.fooddelivery.controller;

import com.dmg.fooddelivery.dto.UserDtos.CreateUser;
import com.dmg.fooddelivery.dto.UserDtos.Registration;
import com.dmg.fooddelivery.dto.UserDtos.UserResponse;
import com.dmg.fooddelivery.service.UserService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody Registration input) {
        return userService.register(input);
    }

    @PostMapping("/admin/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(
            @RequestHeader("X-User-Id") long actorId, @Valid @RequestBody CreateUser input) {
        return userService.create(actorId, input);
    }

    @GetMapping("/me")
    public UserResponse me(@RequestHeader("X-User-Id") long actorId) {
        return UserResponse.from(userService.get(actorId));
    }
}
