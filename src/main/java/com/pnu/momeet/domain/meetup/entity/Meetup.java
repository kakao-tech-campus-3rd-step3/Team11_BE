package com.pnu.momeet.domain.meetup.entity;

import com.pnu.momeet.domain.common.entity.BaseEntity;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.meetup.enums.MeetupCategory;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "meetup")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meetup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private MeetupCategory category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "tags", columnDefinition = "TEXT[]")
    private String[] tags;

    @Column(name = "capacity", nullable = false)
    private Integer capacity = 10;

    @Column(name = "score_limit")
    private Integer scoreLimit;

    @Column(name = "location_point", nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point locationPoint;

    @Column(name = "location_text", columnDefinition = "TEXT")
    private String locationText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MeetupStatus status = MeetupStatus.OPEN;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    public Meetup(Member owner, String name, MeetupCategory category, String description,
                  String[] tags, Integer capacity, Integer scoreLimit, Point locationPoint,
                  String locationText, LocalDateTime startAt, LocalDateTime endAt) {
        this.owner = owner;
        this.name = name;
        this.category = category;
        this.description = description;
        this.tags = tags;
        this.capacity = capacity != null ? capacity : 10;
        this.scoreLimit = scoreLimit;
        this.locationPoint = locationPoint;
        this.locationText = locationText;
        this.status = MeetupStatus.OPEN;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public void updateMeetup(String name, MeetupCategory category, String description,
                            String[] tags, Integer capacity, Integer scoreLimit,
                            Point locationPoint, String locationText, MeetupStatus status,
                            LocalDateTime startAt, LocalDateTime endAt) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.tags = tags;
        this.capacity = capacity;
        this.scoreLimit = scoreLimit;
        this.locationPoint = locationPoint;
        this.locationText = locationText;
        this.status = status;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public void changeStatus(MeetupStatus status) {
        this.status = status;
    }
}
