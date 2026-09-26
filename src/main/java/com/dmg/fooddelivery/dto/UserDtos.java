package com.dmg.fooddelivery.dto;

import com.dmg.fooddelivery.model.Role;
import com.dmg.fooddelivery.model.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDtos {

    private UserDtos() {}

    public record Registration(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,80}") String username,
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "\\+?[1-9][0-9]{7,14}") String phoneNumber) {}

    public record CreateUser(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,80}") String username,
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "\\+?[1-9][0-9]{7,14}") String phoneNumber,
            @NotNull Role role) {}

    public record UserResponse(
            long id,
            String username,
            String fullName,
            String email,
            String phoneNumber,
            Role role) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhoneNumber(),
                    user.getRole());
        }
    }
}
