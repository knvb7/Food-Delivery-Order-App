package com.dmg.fooddelivery.service.impl;

import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.dto.UserDtos.CreateUser;
import com.dmg.fooddelivery.dto.UserDtos.Registration;
import com.dmg.fooddelivery.dto.UserDtos.UserResponse;
import com.dmg.fooddelivery.model.Role;
import com.dmg.fooddelivery.model.User;
import com.dmg.fooddelivery.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void saveAssignsGeneratedId() {
        lenient()
                .when(userRepository.save(any(User.class)))
                .thenAnswer(
                        invocation -> {
                            User user = invocation.getArgument(0);
                            user.setId(99L);
                            return user;
                        });
    }

    @Test
    void getReturnsStoredUser() {
        User storedUser = user(7, Role.CUSTOMER);
        when(userRepository.findById(7L)).thenReturn(Optional.of(storedUser));

        assertSame(storedUser, userService.get(7));
    }

    @Test
    void getRejectsUnknownUser() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> userService.get(404));

        assertEquals(404, exception.status().value());
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void registrationNormalizesProfileAndAlwaysCreatesCustomer() {
        Registration input =
                new Registration(
                        "  new.customer  ",
                        "  New Customer  ",
                        "  New.Customer@Example.COM  ",
                        "  +919876543210  ");

        UserResponse response = userService.register(input);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("new.customer", savedUser.getUsername());
        assertEquals("New Customer", savedUser.getFullName());
        assertEquals("new.customer@example.com", savedUser.getEmail());
        assertEquals("+919876543210", savedUser.getPhoneNumber());
        assertEquals(Role.CUSTOMER, savedUser.getRole());
        assertEquals(99, response.id());
    }

    @Test
    void adminCanCreateAnyRequestedRole() {
        User admin = user(1, Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        CreateUser input =
                new CreateUser(
                        "owner3",
                        "Owner Three",
                        "owner3@example.com",
                        "+919876543211",
                        Role.OWNER);

        UserResponse response = userService.create(1, input);

        assertEquals(Role.OWNER, response.role());
        assertEquals("owner3", response.username());
    }

    @Test
    void nonAdminCannotCreateUsers() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(user(4, Role.CUSTOMER)));
        CreateUser input =
                new CreateUser(
                        "owner3",
                        "Owner Three",
                        "owner3@example.com",
                        "+919876543211",
                        Role.OWNER);

        ApiException exception = assertThrows(ApiException.class, () -> userService.create(4, input));

        assertEquals(403, exception.status().value());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void duplicateUsernameStopsRegistrationBeforeOtherChecks() {
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                userService.register(
                                        registration(
                                                "existing",
                                                "available@example.com",
                                                "+919876543212")));

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void duplicateEmailUsesNormalizedAddress() {
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                userService.register(
                                        registration(
                                                "available",
                                                "  Existing@Example.COM ",
                                                "+919876543213")));

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void duplicatePhoneNumberUsesTrimmedValue() {
        when(userRepository.existsByPhoneNumber("+919876543214")).thenReturn(true);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                userService.register(
                                        registration(
                                                "available",
                                                "available@example.com",
                                                "  +919876543214  ")));

        assertEquals("Phone number already exists", exception.getMessage());
        verify(userRepository).existsByPhoneNumber("+919876543214");
        verify(userRepository, never()).save(any(User.class));
    }

    private static Registration registration(String username, String email, String phoneNumber) {
        return new Registration(username, "Available User", email, phoneNumber);
    }

    private static User user(long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }
}
