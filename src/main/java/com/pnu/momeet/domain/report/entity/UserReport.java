package com.pnu.momeet.domain.report.entity;

import static lombok.AccessLevel.PROTECTED;

import com.pnu.momeet.domain.common.entity.BaseEntity;
import com.pnu.momeet.domain.report.enums.ReportCategory;
import com.pnu.momeet.domain.report.enums.ReportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(name = "user_report")
public class UserReport extends BaseEntity {

    @Column(name = "reporter_profile_id", nullable = false)
    private UUID reporterProfileId;

    @Column(name = "target_profile_id", nullable = false)
    private UUID targetProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ReportCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status = ReportStatus.OPEN;

    @Column(name = "detail")
    private String detail;

    @Column(name = "ip_hash", length = 128)
    private String ipHash;

    @Column(name = "admin_reply")
    private String adminReply;

    @Column(name = "processed_by")
    private UUID processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public static UserReport create(
        UUID reporterId,
        UUID targetId,
        ReportCategory category,
        String detail,
        String ipHash
    ) {
        UserReport entity = new UserReport();
        entity.reporterProfileId = reporterId;
        entity.targetProfileId = targetId;
        entity.category = category;
        entity.status = ReportStatus.OPEN;
        entity.detail = detail;
        entity.ipHash = ipHash;
        return entity;
    }

    public void processReport(UUID adminMemberId, String reply) {
        this.status = ReportStatus.ENDED;
        this.adminReply = (reply == null || reply.isBlank()) ? null : reply.strip();
        this.processedBy = adminMemberId;
        this.processedAt = LocalDateTime.now();
    }
}