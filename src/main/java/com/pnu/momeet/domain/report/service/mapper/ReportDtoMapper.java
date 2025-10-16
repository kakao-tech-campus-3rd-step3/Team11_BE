package com.pnu.momeet.domain.report.service.mapper;

import com.pnu.momeet.domain.report.dto.request.ReportCreateRequest;
import com.pnu.momeet.domain.report.entity.ReportAttachment;
import com.pnu.momeet.domain.report.entity.UserReport;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReportDtoMapper {

    public static UserReport toUserReport(
        UUID reporterProfileId,
        ReportCreateRequest request,
        String ipHash
    ) {
        return UserReport.create(
            reporterProfileId,
            request.targetProfileId(),
            request.category(),
            request.detail(),
            ipHash
        );
    }

    public static ReportAttachment toAttachment(UUID id, String url) {
        return ReportAttachment.create(id, url);
    }
}
