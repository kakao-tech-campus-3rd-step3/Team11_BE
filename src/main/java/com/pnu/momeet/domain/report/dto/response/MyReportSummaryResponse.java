package com.pnu.momeet.domain.report.dto.response;

import com.pnu.momeet.domain.report.enums.ReportCategory;
import com.pnu.momeet.domain.report.enums.ReportStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record MyReportSummaryResponse(
    UUID reportId,
    UUID targetProfileId,
    ReportCategory category,
    ReportStatus status,
    LocalDateTime createdAt
) {}
