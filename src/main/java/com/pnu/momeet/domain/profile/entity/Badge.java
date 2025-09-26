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
@Table(name = "badge")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Badge extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 255)
    private String iconUrl;

    @OneToMany(mappedBy = "badge", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BadgeConditionMapping> conditions = new ArrayList<>();
}
