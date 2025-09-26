package com.pnu.momeet.domain.profile.entity;

import com.pnu.momeet.domain.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "badge_condition_mapping")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class BadgeConditionMapping extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condition_id", nullable = false)
    private BadgeCondition condition;

    @Column(nullable = false, length = 10)
    private String operator = "AND"; // 조건 결합 방식 (AND / OR)
}
