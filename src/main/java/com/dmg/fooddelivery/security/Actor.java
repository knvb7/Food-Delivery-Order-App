package com.dmg.fooddelivery.security;

import com.dmg.fooddelivery.common.ApiException;

public record Actor(long id, String username, Role role) {
    public void require(Role required) {
        if (role != required) throw ApiException.forbidden();
    }

    public void requireOwner(long ownerId) {
        if (role != Role.ADMIN && (role != Role.OWNER || id != ownerId)) throw ApiException.forbidden();
    }
}
