package com.dmg.fooddelivery.dto;

import com.dmg.fooddelivery.model.Role;
import com.dmg.fooddelivery.model.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class UserDtos {

    private UserDtos() {}

    public record Registration(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,80}") String username) {}

    public record CreateUser(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,80}") String username,
            @NotNull Role role) {}

    public record UserResponse(long id, String username, Role role) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getRole());
        }
    }
}
