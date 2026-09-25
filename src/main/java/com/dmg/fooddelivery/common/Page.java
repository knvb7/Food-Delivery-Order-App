package com.dmg.fooddelivery.common;

import java.util.List;

public record Page<T>(List<T> content, int page, int size, long totalElements) {
    public static int offset(int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) {
            throw ApiException.badRequest("page must be 0..100000 and size must be 1..100");
        }
        return page * size;
    }
}
