package com.pnu.momeet.domain.meetup.entity;

import com.pnu.momeet.domain.common.entity.SimpleCreationEntity;
import com.pnu.momeet.domain.meetup.enums.MainCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meetup_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetupCategory extends SimpleCreationEntity {

    @Column(length = 30, nullable = false, unique = true)
    @Enumerated(EnumType.STRING)
    private MainCategory name;
}