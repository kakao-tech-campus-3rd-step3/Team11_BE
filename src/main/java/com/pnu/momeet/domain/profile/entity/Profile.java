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
        this.memberId = memberId;
        this.nickname = nickname;
        this.age = age;
        this.gender = gender;
        this.imageUrl = imageUrl;
        this.description = description;
        this.baseLocation = baseLocation;
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

    public void updateProfile(
        String nickname,
        Integer age,
        Gender gender,
        String imageUrl,
        String description,
        String baseLocation
    ) {
        if (nickname == null) this.nickname = nickname;
        if (age == null) this.age = age;
        if (gender == null) this.gender = gender;
        if (imageUrl == null) this.imageUrl = imageUrl;
        if (description == null) this.description = description;
        if (baseLocation == null) this.baseLocation = baseLocation;
    }

    public void increaseLikes() {
        this.likes++;
    }

    public void increaseDislikes() {
        this.dislikes++;
    }
}
