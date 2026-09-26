package com.dmg.fooddelivery.service.impl;

import com.dmg.fooddelivery.common.Access;
import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.dto.UserDtos.CreateUser;
import com.dmg.fooddelivery.dto.UserDtos.Registration;
import com.dmg.fooddelivery.dto.UserDtos.UserResponse;
import com.dmg.fooddelivery.model.Role;
import com.dmg.fooddelivery.model.User;
import com.dmg.fooddelivery.repository.UserRepository;
import com.dmg.fooddelivery.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User get(long id) {
        return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User"));
    }

    @Override
    @Transactional
    public UserResponse register(Registration input) {
        return save(
                input.username(),
                input.fullName(),
                input.email(),
                input.phoneNumber(),
                Role.CUSTOMER);
    }

    @Override
    @Transactional
    public UserResponse create(long actorId, CreateUser input) {
        Access.requireRole(get(actorId), Role.ADMIN);

        return save(
                input.username(),
                input.fullName(),
                input.email(),
                input.phoneNumber(),
                input.role());
    }

    private UserResponse save(
            String username, String fullName, String email, String phoneNumber, Role role) {
        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedPhoneNumber = phoneNumber.trim();

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw ApiException.conflict("Username already exists");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw ApiException.conflict("Email already exists");
        }

        if (userRepository.existsByPhoneNumber(normalizedPhoneNumber)) {
            throw ApiException.conflict("Phone number already exists");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPhoneNumber(normalizedPhoneNumber);
        user.setRole(role);

        return UserResponse.from(userRepository.save(user));
    }
}
