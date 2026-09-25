package com.dmg.fooddelivery.service;

import com.dmg.fooddelivery.dto.UserDtos.*;
import com.dmg.fooddelivery.model.User;

public interface UserService {
    User get(long id);
    UserResponse register(Registration input);
    UserResponse create(long actorId, CreateUser input);
}
