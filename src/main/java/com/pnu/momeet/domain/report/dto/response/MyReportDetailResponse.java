package com.pnu.momeet.domain.report.dto.response;

import com.pnu.momeet.domain.report.enums.ReportCategory;
import com.pnu.momeet.domain.report.enums.ReportStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MyReportDetailResponse(
    UUID reportId,
    UUID reporterProfileId,
    UUID targetProfileId,
    ReportCategory category,
    ReportStatus status,
    String detail,
    List<String> images,
    LocalDateTime createdAt
) {}
