package com.pnu.momeet.domain.report.service;

import com.pnu.momeet.domain.report.entity.ReportAttachment;
import com.pnu.momeet.domain.report.entity.UserReport;
import com.pnu.momeet.domain.report.enums.ReportStatus;
import com.pnu.momeet.domain.report.repository.ReportAttachmentRepository;
import com.pnu.momeet.domain.report.repository.ReportRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportEntityService {

    private final ReportRepository reportRepository;
    private final ReportAttachmentRepository reportAttachmentRepository;

    @Transactional(readOnly = true)
    public UserReport getById(UUID reportId) {
        log.debug("신고 조회 시도. reportId={}", reportId);
        Optional<UserReport> report = reportRepository.findById(reportId);
        if (report.isEmpty()) {
            log.info("존재하지 않은 신고 조회 시도. reportId={}", reportId);
            throw new NoSuchElementException("존재하지 않는 신고입니다.");
        }
        log.debug("신고 조회 성공. reportId={}", reportId);
        return report.get();
    }

    @Transactional(readOnly = true)
    public Page<UserReport> getOpenReports(PageRequest pageRequest) {
        log.debug("OPEN 상태의 신고 목록 조회 시도.");
        Page<UserReport> reports = reportRepository.findByStatus(ReportStatus.OPEN, pageRequest);
        log.debug("OPEN 상태의 신고 목록 조회 성공.");
        return reports;
    }

    @Transactional(readOnly = true)
    public Page<UserReport> getMyReports(UUID id, PageRequest pageRequest) {
        log.debug("신고 목록 조회 시도. reporterPid={}", id);
        Page<UserReport> reports = reportRepository.findByReporterProfileId(id, pageRequest);
        log.debug("신고 목록 조회 성공. reporterPid={}, count={}", id, reports.getTotalElements());
        return reports;
    }

    @Transactional(readOnly = true)
    public List<String> getAttachmentUrls(UUID reportId) {
        log.debug("신고 첨부파일 URL 조회 시도. reportId={}", reportId);
        List<String> urls = reportAttachmentRepository.findByReportId(reportId)
            .stream().map(ReportAttachment::getUrl).toList();
        log.debug("신고 첨부파일 URL 조회 성공. reportId={}, count={}", reportId, urls.size());
        return urls;
    }

    @Transactional(readOnly = true)
    public List<ReportAttachment> getAttachmentsByReportId(UUID reportId) {
        log.debug("신고 첨부파일 조회 시도. reportId={}", reportId);
        List<ReportAttachment> attachments = reportAttachmentRepository.findByReportId(reportId);
        log.debug("신고 첨부파일 조회 성공. reportId={}, count={}", reportId, attachments.size());
        return attachments;
    }

    @Transactional(readOnly = true)
    public Optional<UserReport> getLastByPair(UUID reporterPid, UUID targetPid) {
        Optional<UserReport> found = reportRepository
            .findTopByReporterProfileIdAndTargetProfileIdOrderByCreatedAtDesc(reporterPid, targetPid);
        log.debug("최근 evaluator→target 평가 조회. reporterPid={}, targetPid={}, exists={}",
            reporterPid, targetPid, found.isPresent());
        return found;
    }

    @Transactional(readOnly = true)
    public boolean existsById(UUID reportId) {
        boolean exists = reportRepository.existsById(reportId);
        log.debug("신고 존재 여부 조회. reportId={}, exists={}", reportId, exists);
        return exists;
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

    @Transactional
    public void deleteReport(UUID memberId, UUID reportId) {
        log.debug("신고 삭제 시도. memberId={}, reportId={}", memberId, reportId);
        reportRepository.deleteById(reportId);
        log.debug("신고 삭제 성공. memberId={}, reportId={}", memberId, reportId);
    }
}
