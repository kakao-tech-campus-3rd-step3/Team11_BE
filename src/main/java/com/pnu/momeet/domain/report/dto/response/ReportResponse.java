package com.pnu.momeet.domain.report.dto.response;

import com.pnu.momeet.domain.report.enums.ReportCategory;
import com.pnu.momeet.domain.report.enums.ReportStatus;
import java.util.List;
import java.util.UUID;

public record ReportResponse(
    UUID reportId,
    UUID reporterProfileId,
    UUID targetProfileId,
    ReportCategory category,
    ReportStatus status,
    List<String> images,
    String detail
) {
}
