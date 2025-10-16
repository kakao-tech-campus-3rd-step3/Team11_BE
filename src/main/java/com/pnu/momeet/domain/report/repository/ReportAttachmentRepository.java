package com.pnu.momeet.domain.report.repository;

import com.pnu.momeet.domain.report.entity.ReportAttachment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportAttachmentRepository extends JpaRepository<ReportAttachment, UUID> {

}
