package com.pnu.momeet.domain.sigungu.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Table(name="sigungu_boundary", schema = "public")
@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sigungu {
    @Id
    @Column(name = "sgg_code", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "sido_code", nullable = false)
    private Long sidoCode;

    @Size(max = 255)
    @NotNull
    @Column(name = "sido_name", nullable = false)
    private String sidoName;

    @Size(max = 255)
    @NotNull
    @Column(name = "sgg_name", nullable = false)
    private String sigunguName;


    @Column(name="geom", columnDefinition = "geometry(Polygon, 4326)")
    private Polygon area;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point baseLocation;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
