package com.pnu.momeet.domain.sigungu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Table(name="sigungu_boundary")
@Setter
@Getter
public class Sigungu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="sido_name")
    private String sidoName;

    @Column(name="sido_code")
    private String sidoCode;

    @Column(name="sgg_name")
    private String sigunguName;

    @Column(name="sgg_code", unique = true)
    private String sigunguCode;

    @Column(name="geom", columnDefinition = "geometry(Polygon, 4326)")
    private Polygon area;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point baseLocation;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
