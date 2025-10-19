package com.pnu.momeet.unit.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.domain.report.entity.ReportAttachment;
import com.pnu.momeet.domain.report.entity.UserReport;
import com.pnu.momeet.domain.report.enums.ReportCategory;
import com.pnu.momeet.domain.report.enums.ReportStatus;
import com.pnu.momeet.domain.report.repository.ReportAttachmentRepository;
import com.pnu.momeet.domain.report.repository.ReportRepository;
import com.pnu.momeet.domain.report.service.ReportEntityService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ReportEntityServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportAttachmentRepository attachmentRepository;

    @InjectMocks
    private ReportEntityService entityService;

    @Test
    @DisplayName("markEnded - 상태 변경 후 저장 호출")
    void markEnded_save() {
        UUID adminProfileId = UUID.randomUUID();
        UserReport report = UserReport.create(java.util.UUID.randomUUID(),
            java.util.UUID.randomUUID(), ReportCategory.ABUSE, "d", "ip");

        entityService.processReport(report, adminProfileId, "메모");

        assertThat(report.getStatus()).isEqualTo(ReportStatus.ENDED);
        assertThat(report.getAdminReply()).isEqualTo("메모");
        assertThat(report.getProcessedBy()).isEqualTo(adminProfileId);

        verify(reportRepository).save(report);
    }

    @Test
    @DisplayName("getOpenReports - findByStatus(OPEN, pageable) 위임/반환 검증")
    void getOpenReports_delegates_findByStatusOpen() {
        PageRequest page = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt"));
        given(reportRepository.findByStatus(ReportStatus.OPEN, page))
            .willReturn(new PageImpl<>(List.of(), page, 0));

        var result = entityService.getOpenReports(page);

        assertThat(result.getTotalElements()).isZero();
        verify(reportRepository).findByStatus(ReportStatus.OPEN, page);
    }

    @Test
    @DisplayName("getAttachmentUrls - URL 리스트 매핑/반환")
    void getAttachmentUrls_success() {
        UUID reportId = UUID.randomUUID();
        var a = ReportAttachment.create(reportId, "https://cdn/reports/a.png");
        var b = ReportAttachment.create(reportId, "https://cdn/reports/b.png");

        given(attachmentRepository.findByReportId(reportId)).willReturn(List.of(a, b));

        List<String> urls = entityService.getAttachmentUrls(reportId);

        assertThat(urls).containsExactlyInAnyOrder("https://cdn/reports/a.png", "https://cdn/reports/b.png");
        verify(attachmentRepository).findByReportId(reportId);
    }

    @Test
    @DisplayName("getMyReports - 레포지토리 위임 및 Pageable 전달 검증(createdAt DESC)")
    void getMyReports_delegates_withSortDesc() {
        // given
        UUID reporterPid = UUID.randomUUID();
        PageRequest pr = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt"));

        UserReport r1 = UserReport.create(reporterPid, UUID.randomUUID(), ReportCategory.SPAM, "d1", "ip");
        UserReport r2 = UserReport.create(reporterPid, UUID.randomUUID(), ReportCategory.ABUSE, "d2", "ip");

        given(reportRepository.findByReporterProfileId(eq(reporterPid), eq(pr)))
            .willReturn(new PageImpl<>(List.of(r2, r1), pr, 2));

        // when
        var page = entityService.getMyReports(reporterPid, pr);

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).containsExactly(r2, r1);
        verify(reportRepository).findByReporterProfileId(reporterPid, pr);
    }

    @Test
    @DisplayName("getLastByPair - 최신 신고 Optional 반환")
    void getLastByPair_returnsOptional() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        UserReport last = UserReport.create(a, b, ReportCategory.SPAM, "도배", "ip");
        given(reportRepository.findTopByReporterProfileIdAndTargetProfileIdOrderByCreatedAtDesc(a, b))
            .willReturn(Optional.of(last));

        Optional<UserReport> found = entityService.getLastByPair(a, b);

        assertThat(found).isPresent();
        verify(reportRepository).findTopByReporterProfileIdAndTargetProfileIdOrderByCreatedAtDesc(a, b);
    }

    @Test
    @DisplayName("existsByReporterTargetIpAfter - 위임/전달 값 확인")
    void existsByReporterTargetIpAfter_delegates() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        String hash = "iphash";
        LocalDateTime after = LocalDateTime.now().minusHours(6);

        given(reportRepository.existsByReporterProfileIdAndTargetProfileIdAndIpHashAndCreatedAtAfter(
            a, b, hash, after)
        ).willReturn(true);

        boolean exists = entityService.existsByReporterTargetIpAfter(a, b, hash, after);

        assertThat(exists).isTrue();
        verify(reportRepository)
            .existsByReporterProfileIdAndTargetProfileIdAndIpHashAndCreatedAtAfter(a, b, hash, after);
    }

    @Test
    @DisplayName("save(UserReport) - 레포 위임")
    void save_report() {
        UserReport report = UserReport.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            ReportCategory.ABUSE,
            "욕설",
            "ip"
        );
        given(reportRepository.save(report)).willReturn(report);

        UserReport saved = entityService.save(report);

        assertThat(saved).isSameAs(report);
        verify(reportRepository).save(report);
    }

    @Test
    @DisplayName("save(ReportAttachment) - 레포 위임")
    void save_attachment() {
        ReportAttachment att = ReportAttachment.create(UUID.randomUUID(), "https://cdn/reports/x.png");
        given(attachmentRepository.save(att)).willReturn(att);

        ReportAttachment saved = entityService.save(att);

        assertThat(saved).isSameAs(att);
        verify(attachmentRepository).save(att);
    }

    @Test
    @DisplayName("getById - 존재하지 않으면 NoSuchElementException")
    void getById_notFound_throws() {
        UUID id = UUID.randomUUID();
        given(reportRepository.findById(id)).willReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> entityService.getById(id))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("deleteReport(memberId, reportId) - 레포 deleteById 위임")
    void deleteReport_delegatesToRepository() {
        UUID memberId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();

        entityService.deleteReport(memberId, reportId);

        verify(reportRepository).deleteById(reportId);
    }

    @Test
    @DisplayName("getAttachmentsByReportId - 첨부 조회 위임")
    void getAttachmentsByReportId_delegates() {
        UUID reportId = UUID.randomUUID();

        entityService.getAttachmentsByReportId(reportId);

        verify(attachmentRepository).findByReportId(reportId);
    }
}
