package com.pnu.momeet.domain.meetup.dto;

import com.pnu.momeet.domain.meetup.enums.MeetupCategory;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MeetupCreateRequest {

    @NotBlank(message = "모임명은 필수입니다")
    @Size(min = 1, max = 60, message = "모임명은 1-60자 사이여야 합니다")
    private String name;

    @NotNull(message = "카테고리는 필수입니다")
    private MeetupCategory category;

    private String description;

    private String[] tags;

    private String[] hashTags;

    @Min(value = 2, message = "최소 인원은 2명입니다")
    @Max(value = 100, message = "최대 인원은 100명입니다")
    private Integer capacity;

    @Min(value = 0, message = "점수 제한은 0 이상이어야 합니다")
    @Max(value = 100, message = "점수 제한은 100 이하여야 합니다")
    private Integer scoreLimit;

    @NotNull(message = "시작 시간은 필수입니다")
    private LocalDateTime startAt;

    @NotNull(message = "종료 시간은 필수입니다")
    private LocalDateTime endAt;

    @NotNull(message = "위치 정보는 필수입니다")
    private LocationRequest location;

    @Getter
    public static class LocationRequest {
        @NotNull(message = "위도는 필수입니다")
        @DecimalMin(value = "32.0", message = "위도는 32도 이상이어야 합니다 (한국 남단 기준)")
        @DecimalMax(value = "39.0", message = "위도는 39도 이하여야 합니다 (한국 북단 기준)")
        private Double latitude;

        @NotNull(message = "경도는 필수입니다")
        @DecimalMin(value = "124.0", message = "경도는 124도 이상이어야 합니다 (한국 서단 기준)")
        @DecimalMax(value = "132.0", message = "경도는 132도 이하여야 합니다 (한국 동단 기준)")
        private Double longitude;

        @NotBlank(message = "주소는 필수입니다")
        private String address;

        public LocationRequest() {}

        public LocationRequest(Double latitude, Double longitude, String address) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
        }
    }

    public MeetupCreateRequest() {}

    public MeetupCreateRequest(String name, MeetupCategory category, String description,
                              String[] tags, String[] hashTags, Integer capacity, Integer scoreLimit,
                              LocalDateTime startAt, LocalDateTime endAt, LocationRequest location) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.tags = tags;
        this.hashTags = hashTags;
        this.capacity = capacity;
        this.scoreLimit = scoreLimit;
        this.startAt = startAt;
        this.endAt = endAt;
        this.location = location;
    }
}
