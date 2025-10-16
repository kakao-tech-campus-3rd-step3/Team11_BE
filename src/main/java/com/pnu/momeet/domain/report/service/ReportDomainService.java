package com.pnu.momeet.domain.report.service;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import com.pnu.momeet.domain.report.dto.request.ReportCreateRequest;
import com.pnu.momeet.domain.report.dto.response.ReportResponse;
import com.pnu.momeet.domain.report.entity.ReportAttachment;
import com.pnu.momeet.domain.report.entity.UserReport;
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

    @Transactional
    public ReportResponse createReport(UUID memberId, ReportCreateRequest request, String ipHash) {
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
        return ReportEntityMapper.toReportResponse(report, uploadedUrls);
    }
}
