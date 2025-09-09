package com.pnu.momeet.domain.common.mapper;

import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public class PageMapper {

    private PageMapper() {
        // private constructor to prevent instantiation
    }

    public static Sort toSort(String sortValue) {
        if (sortValue == null || sortValue.isEmpty()) {
            return Sort.unsorted();
        }

        String[] sortParams = sortValue.split(",");
        List<Sort.Order> orders = new ArrayList<>();

        for (int i = 0; i < sortParams.length / 2; i++) {
            String field = sortParams[2 * i].trim();
            String direction = sortParams[2 * i + 1].trim().toLowerCase();
            var dir = Sort.Direction.valueOf(direction.toUpperCase());
            orders.add(new Sort.Order(dir, field));
        }

        return Sort.by(orders);
    }
}
