package com.pnu.momeet.domain.report.repository;

import com.pnu.momeet.domain.report.entity.UserReport;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<UserReport, UUID> {

    Optional<UserReport> findTopByReporterProfileIdAndTargetProfileIdOrderByCreatedAtDesc(
        UUID reporterPid,
        UUID targetPid
    );

    boolean existsByReporterProfileIdAndTargetProfileIdAndIpHashAndCreatedAtAfter(
        UUID reporterPId,
        UUID targetPid,
        String ipHash,
        LocalDateTime after
    );

    Page<UserReport> findByReporterProfileId(UUID id, Pageable pageable);
}
