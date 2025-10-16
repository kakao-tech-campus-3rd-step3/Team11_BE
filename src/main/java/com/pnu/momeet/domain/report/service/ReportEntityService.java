package com.pnu.momeet.domain.report.service;

import com.pnu.momeet.domain.report.entity.ReportAttachment;
import com.pnu.momeet.domain.report.entity.UserReport;
import com.pnu.momeet.domain.report.repository.ReportAttachmentRepository;
import com.pnu.momeet.domain.report.repository.ReportRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportEntityService {

    private final ReportRepository reportRepository;
    private final ReportAttachmentRepository reportAttachmentRepository;

    @Transactional(readOnly = true)
    public Optional<UserReport> getLastByPair(UUID reporterPid, UUID targetPid) {
        Optional<UserReport> found = reportRepository
            .findTopByReporterProfileIdAndTargetProfileIdOrderByCreatedAtDesc(reporterPid, targetPid);
        log.debug("최근 evaluator→target 평가 조회. reporterPid={}, targetPid={}, exists={}",
            reporterPid, targetPid, found.isPresent());
        return found;
    }

    @Transactional(readOnly = true)
    public boolean existsByReporterTargetIpAfter(
        UUID reporterPId,
        UUID targetPid,
        String ipHash,
        LocalDateTime after
    ) {
        boolean exists = reportRepository
            .existsByReporterProfileIdAndTargetProfileIdAndIpHashAndCreatedAtAfter(
                reporterPId,
                targetPid,
                ipHash,
                after
            );
        log.debug("동일 IP 쿨타임 내 중복 여부. reporterPid={}, targetPid={}, after={}, exists={}",
            reporterPId, targetPid, after, exists);
        return exists;
    }

    @Transactional
    public UserReport save(UserReport userReport) {
        log.debug("신고 저장 시도. id={}, reporterPid={}, targetPid={}, category={}, status={}, detail={}, ipHashLen={}",
            userReport.getId(),
            userReport.getReporterProfileId(),
            userReport.getTargetProfileId(),
            userReport.getCategory(),
            userReport.getStatus(),
            userReport.getDetail(),
            (userReport.getIpHash() == null ? 0 : userReport.getIpHash().length())
        );
        UserReport saved = reportRepository.save(userReport);
        log.debug("신고 저장 성공. id={}, reporterPid={}, targetPid={}, category={}, status={}, detail={}, ipHashLen={}",
            saved.getId(),
            saved.getReporterProfileId(),
            saved.getTargetProfileId(),
            saved.getCategory(),
            saved.getStatus(),
            saved.getDetail(),
            saved.getIpHash()
        );
        return saved;
    }

    @Transactional
    public ReportAttachment save(ReportAttachment attachment) {
        log.debug("신고 첨부파일 저장 시도. id={}, reportId={}, url={}",
            attachment.getId(),
            attachment.getReportId(),
            attachment.getUrl()
        );
        ReportAttachment saved = reportAttachmentRepository.save(attachment);
        log.debug("신고 첨부파일 저장 성공. id={}, reportId={}, url={}",
            attachment.getId(),
            attachment.getReportId(),
            attachment.getUrl()
        );
        return saved;
    }
}
