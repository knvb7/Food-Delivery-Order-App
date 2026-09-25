package com.dmg.fooddelivery.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AccountController {
    private final Accounts accounts;
    public AccountController(Accounts accounts) { this.accounts = accounts; }

    public record Registration(@NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,80}") String username,
                               @NotBlank @Size(min = 10, max = 64) @Pattern(regexp = "[\\x20-\\x7E]+") String password) {}
    public record NewUser(@NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,80}") String username,
                          @NotBlank @Size(min = 10, max = 64) @Pattern(regexp = "[\\x20-\\x7E]+") String password,
                          @NotNull Role role) {}

    @PostMapping("/customers") @ResponseStatus(HttpStatus.CREATED)
    public Actor register(@Valid @RequestBody Registration request) {
        return accounts.create(request.username(), request.password(), Role.CUSTOMER);
    }

    @PostMapping("/admin/users") @ResponseStatus(HttpStatus.CREATED)
    public Actor create(@Valid @RequestBody NewUser request) {
        return accounts.create(request.username(), request.password(), request.role());
    }

    @GetMapping("/me")
    public Actor me() { return accounts.current(); }
}
