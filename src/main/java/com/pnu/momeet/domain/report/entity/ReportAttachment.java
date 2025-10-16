package com.pnu.momeet.domain.report.entity;

import com.pnu.momeet.domain.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "report_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportAttachment extends BaseCreatedEntity {

    @Column(name = "report_id", nullable = false)
    private UUID reportId;

    @Column(name = "url", nullable = false)
    private String url;

    public static ReportAttachment create(UUID reportId, String url) {
        ReportAttachment attachment = new ReportAttachment();
        attachment.reportId = reportId;
        attachment.url = url;
        return attachment;
    }
}
