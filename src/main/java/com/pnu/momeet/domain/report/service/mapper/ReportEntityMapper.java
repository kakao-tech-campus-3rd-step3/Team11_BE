package com.pnu.momeet.domain.report.service.mapper;

import com.pnu.momeet.domain.report.dto.response.ReportResponse;
import com.pnu.momeet.domain.report.entity.UserReport;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReportEntityMapper {

    public static ReportResponse toReportResponse(
        UserReport userReport,
        List<String> urls
    ) {
        return new ReportResponse(
            userReport.getId(),
            userReport.getReporterProfileId(),
            userReport.getTargetProfileId(),
            userReport.getCategory(),
            userReport.getStatus(),
            urls,
            userReport.getDetail()
        );
    }
}
