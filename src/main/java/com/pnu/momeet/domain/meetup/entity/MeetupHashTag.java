package com.pnu.momeet.domain.meetup.entity;

import com.pnu.momeet.domain.common.entity.SimpleCreationEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "meetup_hash_tag")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetupHashTag extends SimpleCreationEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meetup_id", nullable = false)
    @Setter
    private Meetup meetup;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    public MeetupHashTag(String name, Meetup meetup) {
        this.name = name;
    }
}
