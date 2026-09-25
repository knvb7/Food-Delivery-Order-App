package com.dmg.fooddelivery.service;

import com.dmg.fooddelivery.dto.UserDtos.CreateUser;
import com.dmg.fooddelivery.dto.UserDtos.Registration;
import com.dmg.fooddelivery.dto.UserDtos.UserResponse;
import com.dmg.fooddelivery.model.User;

public interface UserService {

    User get(long id);

    UserResponse register(Registration input);

    UserResponse create(long actorId, CreateUser input);
}
