package com.pnu.momeet.domain.meetup.repository;

import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.profile.entity.Profile;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetupRepository extends JpaRepository<Meetup, UUID>, JpaSpecificationExecutor<Meetup> {

    @Query(value = """
            SELECT * FROM meetup m
            WHERE st_dwithin(m.location_point, :location, :radius * 1000) = true
    """, nativeQuery = true)
    List<Meetup> findAllByDistance(Point location, double radius);

    @Query(value = """
            SELECT * FROM meetup m
            WHERE st_dwithin(m.location_point, :location, :radius * 1000) = true
            AND m.category_id = (
                SELECT id FROM meetup_category WHERE name = :category
            )
    """, nativeQuery = true)
    List<Meetup> findAllByDistanceAndCategory (Point location, double radius, String category);

    @Query(value = """
            SELECT * FROM meetup m
            WHERE st_dwithin(m.location_point, :location, :radius * 1000) = true
            AND m.sub_category_id = (
                SELECT id FROM meetup_sub_category WHERE name = :subCategory
            )
    """, nativeQuery = true)
    List<Meetup> findAllByDistanceAndSubCategory(Point location, double radius, String subCategory);

    @Query(value = """
            SELECT * FROM meetup m
            WHERE st_dwithin(m.location_point, :location, :radius * 1000) = true
            AND (
                LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
    """, nativeQuery = true)
    List<Meetup> findAllByDistanceAndKeyword(Point location, double radius, String keyword);

    @Query(value = """
            SELECT * FROM meetup m
            WHERE st_dwithin(m.location_point, :location, :radius * 1000) = true
            AND m.category_id = (
                SELECT id FROM meetup_category WHERE name = :category
            )
            AND (
                LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
    """, nativeQuery = true)
    List<Meetup> findAllByDistanceAndCategoryAndKeyword(Point location, double radius, String category, String keyword);

    @Query(value = """
            SELECT * FROM meetup m
            WHERE st_dwithin(m.location_point, :location, :radius * 1000) = true
            AND m.sub_category_id = (
                SELECT id FROM meetup_sub_category WHERE name = :subCategory
            )
            AND (
                LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
    """, nativeQuery = true)
    List<Meetup> findAllByDistanceAndSubCategoryAndKeyword(Point location, double radius, String subCategory, String keyword);

    Optional<Meetup> findByOwner(Profile profile);
}