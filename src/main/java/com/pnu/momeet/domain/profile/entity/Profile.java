package com.pnu.momeet.domain.profile.entity;

import com.pnu.momeet.domain.common.entity.BaseEntity;
import com.pnu.momeet.domain.profile.enums.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "profile",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_profile_member_id", columnNames = "member_id")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Profile extends BaseEntity {

    @Column(name = "member_id", nullable = false, columnDefinition = "UUID")
    private UUID memberId;

    @Column(name = "nickname", length = 20, nullable = false)
    private String nickname;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10, nullable = false)
    private Gender gender;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "base_location", length = 100, nullable = false)
    private String baseLocation;

    @Column(name = "temperature", precision = 4, scale = 1, nullable = false)
    private BigDecimal temperature = BigDecimal.valueOf(36.5);

    @Column(name = "likes", nullable = false)
    private int likes = 0;

    @Column(name = "dislikes", nullable = false)
    private int dislikes = 0;

    @Column(name = "un_evaluated_meetup_id")
    private Long unEvaluatedMeetupId;

    private Profile(
        UUID memberId,
        String nickname,
        Integer age,
        Gender gender,
        String imageUrl,
        String description,
        String baseLocation
    ) {
        this.memberId = Objects.requireNonNull(memberId, "memberId는 필수입니다.");
        this.nickname = validateNickname(nickname);
        this.age = validateAge(age);
        this.gender = Objects.requireNonNull(gender, "성별은 필수입니다.");
        this.imageUrl = imageUrl;
        this.description = validateDescription(description);
        this.baseLocation = validateBaseLocation(baseLocation);
    }

    public static Profile create(
        UUID memberId,
        String nickname,
        Integer age,
        Gender gender,
        String imageUrl,
        String description,
        String baseLocation
    ) {
        return new Profile(memberId, nickname, age, gender, imageUrl, description, baseLocation);
    }

    public void updateProfile(String nickname,
        Integer age,
        Gender gender,
        String imageUrl,
        String description,
        String baseLocation) {
        if (nickname != null) this.nickname = validateNickname(nickname);
        if (age != null) this.age = validateAge(age);
        if (gender != null) this.gender = gender;
        if (imageUrl != null) this.imageUrl = validateImageUrl(imageUrl);
        if (description != null) this.description = validateDescription(description);
        if (baseLocation != null) this.baseLocation = validateBaseLocation(baseLocation);
    }

    public void increaseLikes() {
        this.likes++;
    }

    public void increaseDislikes() {
        this.dislikes++;
    }

    private String validateNickname(String raw) {
        if (raw == null) throw new IllegalArgumentException("닉네임은 필수입니다.");
        String trimmed = raw.trim();
        if (trimmed.length() < 2 || trimmed.length() > 20) {
            throw new IllegalArgumentException("닉네임은 2~20자여야 합니다.");
        }
        return trimmed;
    }

    private Integer validateAge(Integer age) {
        if (age == null) throw new IllegalArgumentException("나이는 필수입니다.");
        if (age < 14 || age > 100) {
            throw new IllegalArgumentException("나이는 14~100 사이여야 합니다.");
        }
        return age;
    }

    private String validateImageUrl(String imageUrl) {
        if (imageUrl != null) {
            if (imageUrl.isBlank()) {
                throw new IllegalArgumentException("이미지 URL이 비어 있을 수 없습니다.");
            }
            if (imageUrl.length() > 255) {
                throw new IllegalArgumentException("이미지 URL은 255자 이하여야 합니다.");
            }
        }
        return imageUrl;
    }

    private String validateDescription(String desc) {
        if (desc != null && desc.length() > 500) {
            throw new IllegalArgumentException("소개글은 500자 이하여야 합니다.");
        }
        return desc;
    }

    private String validateBaseLocation(String baseLocation) {
        if (baseLocation == null || baseLocation.isBlank()) {
            throw new IllegalArgumentException("기본 활동 지역은 필수입니다.");
        }
        if (baseLocation.length() > 100) {
            throw new IllegalArgumentException("기본 활동 지역은 100자 이하여야 합니다.");
        }
        return baseLocation;
    }
}
