package com.pnu.momeet.domain.common.mapper;

import java.util.Set;
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

    public static Sort toSortOrDefault(String sortValue, Set<String> allowed, Sort defaultSort) {
        Sort parsed = toSort(sortValue);

        // 화이트리스트 적용
        List<Sort.Order> safe = parsed.stream()
            .filter(o -> allowed.contains(o.getProperty()))
            .toList();

        return safe.isEmpty() ? defaultSort : Sort.by(safe);
    }
}
