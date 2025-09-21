package com.pnu.momeet.domain.meetup.dto.request;

import com.pnu.momeet.common.validation.annotation.ValidMainCategory;
import com.pnu.momeet.common.validation.annotation.ValidSubCategory;
import jakarta.validation.constraints.*;

import java.util.List;

public record MeetupUpdateRequest(
    @Size(min = 1, max = 60, message = "모임명은 1-60자 사이여야 합니다")
    String name,

    @ValidMainCategory
    String category,

    @ValidSubCategory
    String subCategory,

    String description,

    @Size(max = 10, message = "해시태그는 최대 10개까지 가능합니다")
    List<String> hashTags,

    @Min(value = 2, message = "최소 인원은 2명입니다")
    @Max(value = 100, message = "최대 인원은 100명입니다")
    Integer capacity,

    @Min(value = 0, message = "점수 제한은 0 이상이어야 합니다")
    Double scoreLimit,

    LocationRequest location
) {

    public MeetupUpdateRequest(
        String name,
        String category,
        String subCategory,
        String description,
        List<String> hashTags,
        Integer capacity,
        Double scoreLimit,
        LocationRequest location
    ) {
        this.name = name;
        this.category = category != null ? category.toUpperCase() : null;
        this.subCategory = subCategory != null ? subCategory.toUpperCase() : null;
        this.description = description;
        this.hashTags = hashTags;
        this.capacity = capacity;
        this.scoreLimit = scoreLimit;
        this.location = location;
    }

}