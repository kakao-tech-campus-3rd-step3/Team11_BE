package com.pnu.momeet.domain.meetup.entity;


import com.pnu.momeet.domain.common.entity.BaseEntity;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "meetup")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meetup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Profile owner;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private MeetupCategory category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sub_category_id", nullable = false)
    private MeetupSubCategory subCategory;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "capacity", nullable = false)
    private Integer capacity = 10;

    @Column(name = "score_limit")
    private Integer scoreLimit;

    @Column(name = "location_point", nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point locationPoint;

    private String address;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sgg_code", nullable = false)
    private Sigungu sigungu;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MeetupStatus status = MeetupStatus.OPEN;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;
}