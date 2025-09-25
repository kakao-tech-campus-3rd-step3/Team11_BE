package com.pnu.momeet.domain.badge.entity;

import com.pnu.momeet.domain.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "badge")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Badge extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "icon_url", nullable = false, length = 255)
    private String iconUrl;

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    public static Badge create(String name, String description, String iconUrl, String code) {
        return new Badge(name, description, iconUrl, normalizeCode(code));
    }

    public void updateIconUrl(String newImageUrl) {
        this.iconUrl = newImageUrl;
    }

    public void updateBadge(String name, String description) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
    }

    private static String normalizeCode(String raw) {
        return raw == null ? null : raw.trim().toUpperCase();
    }
}
