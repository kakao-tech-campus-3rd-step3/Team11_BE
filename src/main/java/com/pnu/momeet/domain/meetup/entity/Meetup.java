package com.pnu.momeet.domain.meetup.entity;


import com.pnu.momeet.domain.common.entity.BaseEntity;
import com.pnu.momeet.domain.meetup.enums.MainCategory;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import com.pnu.momeet.domain.meetup.enums.SubCategory;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private SubCategory subCategory;

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

    @Column(name = "end_at")
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
            SubCategory subCategory,
            String description,
            Integer capacity,
            Double scoreLimit,
            Point locationPoint,
            String address,
            Sigungu sigungu,
            LocalDateTime endAt
    ) {
        this.owner = owner;
        this.name = name;
        this.category = category;
        this.subCategory = subCategory;
        this.description = description;
        this.capacity = capacity;
        this.scoreLimit = scoreLimit;
        this.locationPoint = locationPoint;
        this.address = address;
        this.sigungu = sigungu;
        this.endAt = endAt;
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
    }

    public void removeParticipant(Participant participant) {
        this.participants.remove(participant);
        participant.setMeetup(null);
    }
}