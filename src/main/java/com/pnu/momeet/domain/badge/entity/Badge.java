package com.pnu.momeet.domain.badge.entity;

import com.pnu.momeet.domain.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "badge")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Badge extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "icon_url", nullable = false, length = 255)
    private String iconUrl;
}
