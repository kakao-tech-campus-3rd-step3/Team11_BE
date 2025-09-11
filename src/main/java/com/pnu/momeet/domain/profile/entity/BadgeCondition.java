package com.pnu.momeet.domain.profile.entity;

import com.pnu.momeet.domain.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "badge_condition")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class BadgeCondition extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code; // 예: FIRST_JOIN, TEN_JOINS

    @Column(length = 255)
    private String description;

    @OneToMany(mappedBy = "condition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BadgeConditionMapping> badges = new ArrayList<>();
}
