package com.pnu.momeet.domain.report.service;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import com.pnu.momeet.domain.report.dto.request.ReportCreateRequest;
import com.pnu.momeet.domain.report.dto.request.ReportPageRequest;
import com.pnu.momeet.domain.report.dto.response.ReportDetailResponse;
import com.pnu.momeet.domain.report.dto.response.ReportSummaryResponse;
import com.pnu.momeet.domain.report.entity.ReportAttachment;
import com.pnu.momeet.domain.report.entity.UserReport;
import com.pnu.momeet.domain.report.enums.ReportStatus;
import com.pnu.momeet.domain.report.service.mapper.ReportDtoMapper;
import com.pnu.momeet.domain.report.service.mapper.ReportEntityMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportDomainService {

    private static final String REPORT_IMAGE_PREFIX = "/reports";
    private static final Duration REPORT_COOLTIME = Duration.ofHours(6);

    private final ReportEntityService entityService;
    private final ProfileEntityService profileService;
    private final S3StorageService s3StorageService;

    @Transactional(readOnly = true)
    public ReportDetailResponse getMyReport(UUID memberId, UUID reportId) {
        Profile me = profileService.getByMemberId(memberId);
        UserReport report = entityService.getById(reportId);
        if (!report.getReporterProfileId().equals(me.getId())) {
            log.info("신고자가 아닌 사용자가 신고 조회 시도. reporterId={}", me.getId());
        }
        List<String> urls = entityService.getAttachmentUrls(reportId);
        log.debug("신고 조회 성공. reporterPid={}, reportId={}", me.getId(), reportId);
        return ReportEntityMapper.toReportDetailResponse(report, urls);
    }

    @Transactional(readOnly = true)
    public Page<ReportSummaryResponse> getMyReports(UUID memberId, ReportPageRequest reportPageRequest) {
        PageRequest pageRequest = ReportDtoMapper.toPageRequest(reportPageRequest);
        Profile reporterProfile = profileService.getByMemberId(memberId);
        Page<UserReport> reports = entityService.getMyReports(reporterProfile.getId(), pageRequest);
        log.debug("신고 목록 조회 성공. reporterPid={}", reporterProfile.getId());
        return reports.map(ReportEntityMapper::toReportSummaryResponse);
    }

    @Transactional
    public ReportDetailResponse createReport(UUID memberId, ReportCreateRequest request, String ipHash) {
        Profile reporterProfile = profileService.getByMemberId(memberId);
        if (!profileService.existsById(request.targetProfileId())) {
            log.info("존재하지 않는 프로필에 대해 신고 생성 시도. memberId={}", memberId);
            throw new NoSuchElementException("존재하지 않는 프로필입니다.");
        }
        if (reporterProfile.getId().equals(request.targetProfileId())) {
            log.info("자기 자신으로 신고 생성 시도. reporterPid={}, targetPid={}",
                reporterProfile.getId(), request.targetProfileId()
            );
            throw new IllegalStateException("자기 자신을 신고할 수 없습니다.");
        }
        entityService.getLastByPair(reporterProfile.getId(), request.targetProfileId())
            .filter(last -> last.getCreatedAt().isAfter(LocalDateTime.now().minus(REPORT_COOLTIME)))
            .ifPresent(x -> {
                log.info("동일 reporter → target 쿨타임 위반. reporterPid={}, targetPid={}",
                    reporterProfile.getId(), request.targetProfileId());
                throw new IllegalStateException("동일 사용자에 대한 신고는 6시간 당 한 번만 가능합니다.");
            });
        if (entityService.existsByReporterTargetIpAfter(
            reporterProfile.getId(), request.targetProfileId(), ipHash, LocalDateTime.now().minus(REPORT_COOLTIME))) {
            log.info("동일 IP 해시 쿨타임 위반. reporterPid={}, targetPid={}",
                reporterProfile.getId(), request.targetProfileId()
            );
            throw new IllegalStateException("동일 위치에서 이미 해당 사용자에 대한 신고가 등록되었습니다.");
        }

        UserReport report = ReportDtoMapper.toUserReport(reporterProfile.getId(), request, ipHash);
        entityService.save(report);

        List<String> uploadedUrls = new ArrayList<>();
        try {
            List<MultipartFile> images = request.images();
            if (images != null && !images.isEmpty()) {
                for (MultipartFile image : images) {
                    String prefix = REPORT_IMAGE_PREFIX + "/" + report.getId();
                    String url = s3StorageService.uploadImage(image, prefix);
                    ReportAttachment attachment = ReportDtoMapper.toAttachment(report.getId(), url);
                    entityService.save(attachment);
                    uploadedUrls.add(url);
                }
            }
        } catch (Exception e) {     // S3 업로드 실패 시 지금까지 업로드 된 이미지 제거
            for (int i = uploadedUrls.size() - 1; i >= 0; i--) {
                String url = uploadedUrls.get(i);
                try {
                    s3StorageService.deleteImage(url);
                } catch (Exception cleanupEx) {
                    log.warn("S3 삭제 실패. url={}", url, cleanupEx);
                }
            }
            throw e;
        }

        log.info("신고 생성 성공. reportId={}, reporterProfileId={}, targetProfileId={}",
            report.getId(), reporterProfile.getId(), request.targetProfileId());
        return ReportEntityMapper.toReportDetailResponse(report, uploadedUrls);
    }

    @Transactional
    public void deleteReport(UUID memberId, UUID reportId) {
        Profile reporterProfile = profileService.getByMemberId(memberId);
        UserReport report = entityService.getById(reportId);
        if (!report.getReporterProfileId().equals(reporterProfile.getId())) {
            log.info("신고 작성자가 아닌 사용자가 신고 삭제 시도. reportId={}, reporterPId={}",
                reportId, reporterProfile.getId()
            );
            throw new IllegalStateException("신고 작성자가 아닙니다.");
        }
        if (report.getStatus() != ReportStatus.OPEN) {
            log.info("OPEN 상태가 아닌 신고 삭제 시도. reportId={}", reportId);
            throw new IllegalStateException("OPEN 상태가 아닌 신고는 삭제할 수 없습니다.");
        }

        List<ReportAttachment> attachments = entityService.getAttachmentsByReportId(reportId);
        for (var att : attachments) {
            try {
                s3StorageService.deleteImage(att.getUrl());
            } catch (Exception e) {
                log.warn("S3 삭제 실패. reportId={}, url={}", reportId, att.getUrl(), e);
            }
        }

        entityService.deleteReport(memberId, reportId);

        log.info("신고 삭제 성공. memberId={}, reportId={}", memberId, reportId);
    }
}
