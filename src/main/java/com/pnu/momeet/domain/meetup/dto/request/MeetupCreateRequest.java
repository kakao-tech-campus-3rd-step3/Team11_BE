package com.pnu.momeet.domain.meetup.dto.request;

import com.pnu.momeet.common.validation.annotation.ValidMainCategory;
import com.pnu.momeet.common.validation.annotation.ValidSubCategory;
import jakarta.validation.constraints.*;

import java.util.List;

public record MeetupCreateRequest(
        @NotBlank(message = "모임명은 필수입니다")
        @Size(min = 1, max = 60, message = "모임명은 1-60자 사이여야 합니다")
        String name,

        @NotNull(message = "카테고리는 필수입니다")
        @ValidMainCategory
        String category,

        @ValidSubCategory
        String subCategory,

        String description,

        @Size(max = 10, message = "해시태그는 최대 10개까지 가능합니다")
        List<String> hashTags,

        @Min(value = 2, message = "최소 인원은 2명입니다")
        @Max(value = 99, message = "최대 인원은 99명입니다")
        Integer capacity,

        @Min(value = 0, message = "점수 제한은 0 이상이어야 합니다")
        Double scoreLimit,

        @NotNull(message = "마감 시간은 필수입니다")
        @Min(value = 1, message = "모임 지속 시간은 최소 1시간 이상이어야 합니다")
        @Max(value = 24, message = "모임 지속 시간은 최대 24시간 이내여야 합니다")
        Integer durationHours,

        @NotNull(message = "위치 정보는 필수입니다")
        LocationRequest location
) {
        public MeetupCreateRequest(
                String name,
                String category,
                String subCategory,
                String description,
                List<String> hashTags,
                Integer capacity,
                Double scoreLimit,
                Integer durationHours,
                LocationRequest location
        ) {
                this.name = name;
                this.category = category.toUpperCase();
                this.subCategory = subCategory != null ? subCategory.toUpperCase() : null;
                this.description = description != null ? description : "";
                this.hashTags = hashTags != null ? hashTags : List.of();
                this.capacity = capacity != null ? capacity : 10;
                this.scoreLimit = scoreLimit != null ? scoreLimit : 0.0;
                this.durationHours = durationHours;
                this.location = location;
        }
}
