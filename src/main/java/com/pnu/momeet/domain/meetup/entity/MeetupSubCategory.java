package com.pnu.momeet.domain.meetup.entity;

import com.pnu.momeet.domain.common.entity.SimpleCreationEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meetup_sub_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetupSubCategory extends SimpleCreationEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private MeetupCategory category;

    @Column(length = 30, nullable = false)
    private String name;
}