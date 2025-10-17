package com.pnu.momeet.domain.report.service.mapper;

import com.pnu.momeet.domain.report.dto.response.ReportSummaryResponse;
import com.pnu.momeet.domain.report.dto.response.ReportDetailResponse;
import com.pnu.momeet.domain.report.entity.UserReport;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReportEntityMapper {

    public static ReportSummaryResponse toReportSummaryResponse(UserReport userReport) {
        return new ReportSummaryResponse(
            userReport.getId(),
            userReport.getTargetProfileId(),
            userReport.getCategory(),
            userReport.getStatus(),
            userReport.getCreatedAt()
        );
    }

    public static ReportDetailResponse toReportDetailResponse(UserReport report, List<String> urls) {
        return new ReportDetailResponse(
            report.getId(),
            report.getReporterProfileId(),
            report.getTargetProfileId(),
            report.getCategory(),
            report.getStatus(),
            report.getDetail(),
            urls,
            report.getCreatedAt(),
            report.getAdminReply(),
            report.getProcessedBy(),
            report.getProcessedAt()
        );
    }
}
