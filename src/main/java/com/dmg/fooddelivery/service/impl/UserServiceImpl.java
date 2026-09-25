package com.dmg.fooddelivery.service.impl;

import com.dmg.fooddelivery.common.*;
import com.dmg.fooddelivery.dto.UserDtos.*;
import com.dmg.fooddelivery.model.*;
import com.dmg.fooddelivery.repository.UserRepository;
import com.dmg.fooddelivery.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository users;

    @Override
    public User get(long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
    }

    @Override
    @Transactional
    public UserResponse register(Registration input) {
        return save(input.username(), Role.CUSTOMER);
    }
    @Override
    @Transactional
    public UserResponse create(long actorId, CreateUser input) {
        Access.requireRole(get(actorId), Role.ADMIN);
        return save(input.username(), input.role());
    }

    private UserResponse save(String username, Role role) {
        if (users.existsByUsername(username)) throw ApiException.conflict("Username already exists");
        var user = new User();
        user.setUsername(username); user.setRole(role);
        return UserResponse.from(users.save(user));
    }
}
