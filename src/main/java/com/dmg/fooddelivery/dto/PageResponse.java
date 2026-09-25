package com.dmg.fooddelivery.dto;

import com.dmg.fooddelivery.common.ApiException;
import java.util.List;
import org.springframework.data.domain.*;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
    public static Pageable request(int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) throw ApiException.badRequest("page must be 0..100000 and size must be 1..100");
        return PageRequest.of(page, size, Sort.by("id").descending());
    }
}
