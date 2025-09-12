package com.pnu.momeet.domain.meetup.repository;

import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.enums.MeetupCategory;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetupRepository extends JpaRepository<Meetup, UUID> {

    // 기본 조회
    Optional<Meetup> findByIdAndOwner_Id(UUID meetupId, UUID ownerId);

    // 상태별 조회
    Page<Meetup> findByStatus(MeetupStatus status, Pageable pageable);

    // 카테고리별 조회
    Page<Meetup> findByCategory(MeetupCategory category, Pageable pageable);

    // 소유자별 조회
    Page<Meetup> findByOwner_Id(UUID ownerId, Pageable pageable);

    // 검색 (이름 + 설명)
    @Query("SELECT m FROM Meetup m WHERE " +
           "(LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(m.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Meetup> findBySearchKeyword(@Param("search") String search, Pageable pageable);

    // 복합 필터 조회 (native query로 PostGIS 지원)
    @Query(value = """
        SELECT m.* FROM meetup m 
        WHERE (:category IS NULL OR m.category = CAST(:category AS VARCHAR))
        AND (:status IS NULL OR m.status = CAST(:status AS VARCHAR))
        AND (:search IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')) 
             OR LOWER(m.description) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:scoreLimit IS NULL OR m.score_limit IS NULL OR m.score_limit <= :scoreLimit)
        AND (:startDate IS NULL OR m.start_at >= :startDate)
        AND (:endDate IS NULL OR m.end_at <= :endDate)
        ORDER BY m.created_at DESC
        """, 
        countQuery = """
        SELECT COUNT(*) FROM meetup m 
        WHERE (:category IS NULL OR m.category = CAST(:category AS VARCHAR))
        AND (:status IS NULL OR m.status = CAST(:status AS VARCHAR))
        AND (:search IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')) 
             OR LOWER(m.description) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:scoreLimit IS NULL OR m.score_limit IS NULL OR m.score_limit <= :scoreLimit)
        AND (:startDate IS NULL OR m.start_at >= :startDate)
        AND (:endDate IS NULL OR m.end_at <= :endDate)
        """,
        nativeQuery = true)
    Page<Meetup> findByFilters(@Param("category") String category,
                              @Param("status") String status,
                              @Param("search") String search,
                              @Param("scoreLimit") Integer scoreLimit,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate,
                              Pageable pageable);

    // 반경 검색 (PostGIS 기능 사용)
    @Query(value = """
        SELECT m.* FROM meetup m 
        WHERE ST_DWithin(m.location_point, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326), :radiusMeters)
        AND (:category IS NULL OR m.category = CAST(:category AS VARCHAR))
        AND (:status IS NULL OR m.status = CAST(:status AS VARCHAR))
        ORDER BY ST_Distance(m.location_point, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326))
        """,
        countQuery = """
        SELECT COUNT(*) FROM meetup m 
        WHERE ST_DWithin(m.location_point, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326), :radiusMeters)
        AND (:category IS NULL OR m.category = CAST(:category AS VARCHAR))
        AND (:status IS NULL OR m.status = CAST(:status AS VARCHAR))
        """,
        nativeQuery = true)
    Page<Meetup> findByLocationRadius(@Param("latitude") Double latitude,
                                     @Param("longitude") Double longitude,
                                     @Param("radiusMeters") Double radiusMeters,
                                     @Param("category") String category,
                                     @Param("status") String status,
                                     Pageable pageable);

    // 태그 검색
    @Query(value = """
        SELECT m.* FROM meetup m 
        WHERE :tag = ANY(m.tags)
        ORDER BY m.created_at DESC
        """, nativeQuery = true)
    Page<Meetup> findByTag(@Param("tag") String tag, Pageable pageable);

    // 여러 태그 검색 (OR 조건)
    @Query(value = """
        SELECT m.* FROM meetup m 
        WHERE m.tags && CAST(:tags AS TEXT[])
        ORDER BY m.created_at DESC
        """, nativeQuery = true)
    Page<Meetup> findByTags(@Param("tags") String[] tags, Pageable pageable);

    // 기간별 조회
    Page<Meetup> findByStartAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // 소유자 확인
    boolean existsByIdAndOwner_Id(UUID meetupId, UUID ownerId);
}
