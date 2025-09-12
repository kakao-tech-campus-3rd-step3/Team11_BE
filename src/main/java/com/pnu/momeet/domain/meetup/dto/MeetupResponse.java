package com.pnu.momeet.domain.meetup.dto;

import com.pnu.momeet.domain.meetup.enums.MeetupCategory;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class MeetupResponse {
    private UUID id;
    private UUID ownerId;
    private OwnerProfile ownerProfile;
    private String name;
    private MeetupCategory category;
    private String description;
    private String[] tags;
    private String[] hashTags;
    private Integer scoreLimit;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private MeetupStatus status;
    private Location location;
    private Integer participantCount;
    private Integer capacity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    public static class OwnerProfile {
        private UUID id;
        private String nickname;
        private String imageUrl;
        private Double temperature;

        public OwnerProfile(UUID id, String nickname, String imageUrl, Double temperature) {
            this.id = id;
            this.nickname = nickname;
            this.imageUrl = imageUrl;
            this.temperature = temperature;
        }
    }

    @Getter
    public static class Location {
        private Double latitude;
        private Double longitude;
        private String address;

        public Location(Double latitude, Double longitude, String address) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
        }
    }

    public MeetupResponse(UUID id, UUID ownerId, OwnerProfile ownerProfile, String name, 
                         MeetupCategory category, String description, String[] tags, String[] hashTags,
                         Integer scoreLimit, LocalDateTime startAt, LocalDateTime endAt,
                         MeetupStatus status, Location location, Integer participantCount,
                         Integer capacity, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerProfile = ownerProfile;
        this.name = name;
        this.category = category;
        this.description = description;
        this.tags = tags;
        this.hashTags = hashTags;
        this.scoreLimit = scoreLimit;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
        this.location = location;
        this.participantCount = participantCount;
        this.capacity = capacity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
