package com.pnu.momeet.domain.meetup.entity;


import com.pnu.momeet.domain.common.entity.BaseEntity;
import com.pnu.momeet.domain.meetup.enums.MainCategory;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "meetup")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meetup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Profile owner;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private MainCategory category;

    @OneToMany(mappedBy = "meetup", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MeetupHashTag> hashTags = new ArrayList<>();

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "participant_count", nullable = false)
    private Integer participantCount = 1;

    @Column(name = "capacity", nullable = false)
    private Integer capacity = 10;

    @Column(name = "score_limit", nullable = false)
    private Double scoreLimit;

    @Column(name = "location_point", nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point locationPoint;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sgg_code", nullable = false)
    private Sigungu sigungu;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MeetupStatus status = MeetupStatus.OPEN;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @OneToMany(mappedBy = "meetup", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, 
            orphanRemoval = true)
    private List<Participant> participants = new ArrayList<>();

    @Builder
    public Meetup(
            Profile owner,
            String name,
            MainCategory category,
            String description,
            Integer capacity,
            Double scoreLimit,
            Point locationPoint,
            String address,
            Sigungu sigungu,
            LocalDateTime startAt,
            LocalDateTime endAt,
            MeetupStatus status
    ) {
        this.owner = owner;
        this.name = name;
        this.category = category;
        this.description = description;
        this.capacity = capacity;
        this.scoreLimit = scoreLimit;
        this.locationPoint = locationPoint;
        this.address = address;
        this.sigungu = sigungu;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = Objects.requireNonNullElse(status, MeetupStatus.OPEN);
        this.participantCount = 0;
    }

    public void setHashTags(List<String> hashTags) {
        this.hashTags.clear();
        for (String tag : hashTags) {
            this.hashTags.add(new MeetupHashTag(tag, this));
        }
    }

    public void addParticipant(Participant participant) {
        this.participants.add(participant);
        participant.setMeetup(this);
        this.participantCount = this.participants.size();
    }

    public void removeParticipant(Participant participant) {
        this.participants.remove(participant);
        participant.setMeetup(null);
        this.participantCount = this.participants.size();
    }
}