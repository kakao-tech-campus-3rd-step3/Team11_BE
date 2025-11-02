package com.pnu.momeet.unit.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import com.pnu.momeet.domain.report.dto.request.ReportCreateRequest;
import com.pnu.momeet.domain.report.dto.request.ReportPageRequest;
import com.pnu.momeet.domain.report.dto.request.ReportProcessRequest;
import com.pnu.momeet.domain.report.dto.response.ReportDetailResponse;
import com.pnu.momeet.domain.report.dto.response.ReportSummaryResponse;
import com.pnu.momeet.domain.report.entity.ReportAttachment;
import com.pnu.momeet.domain.report.entity.UserReport;
import com.pnu.momeet.domain.report.enums.ReportCategory;
import com.pnu.momeet.domain.report.enums.ReportStatus;
import com.pnu.momeet.domain.report.service.ReportDomainService;
import com.pnu.momeet.domain.report.service.ReportEntityService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReportDomainServiceTest {

    @Mock
    private ReportEntityService entityService;
    @Mock private ProfileEntityService profileService;
    @Mock private S3StorageService s3StorageService;

    @InjectMocks
    private ReportDomainService reportService;

    @Test
    @DisplayName("getReportById - 단건/첨부 조회 및 상세 DTO 매핑")
    void getReportByAdmin_success() {
        // given
        UUID reportId = UUID.randomUUID();
        UUID reporter = UUID.randomUUID();
        UUID target   = UUID.randomUUID();

        UserReport report = UserReport.create(reporter, target, ReportCategory.SPAM, "detail", "ip");
        org.springframework.test.util.ReflectionTestUtils.setField(report, "id", reportId);
        org.springframework.test.util.ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.now());

        given(entityService.getById(reportId)).willReturn(report);
        given(entityService.getAttachmentUrls(reportId))
            .willReturn(List.of("https://cdn/r1.png", "https://cdn/r2.png"));

        // when
        ReportDetailResponse resp = reportService.getReportById(reportId);

        // then
        assertThat(resp.reportId()).isEqualTo(reportId);
        assertThat(resp.reporterProfileId()).isEqualTo(reporter);
        assertThat(resp.targetProfileId()).isEqualTo(target);
        assertThat(resp.detail()).isEqualTo("detail");
        assertThat(resp.images()).containsExactly("https://cdn/r1.png", "https://cdn/r2.png");
        assertThat(resp.createdAt()).isNotNull();

        verify(entityService).getById(reportId);
        verify(entityService).getAttachmentUrls(reportId);
    }

    @Test
    @DisplayName("getOpenReports - createdAt DESC 정렬 + DTO 매핑")
    void getOpenReports_success_desc_and_mapping() {
        // given
        PageRequest page = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        UserReport oldOne = UserReport.create(UUID.randomUUID(), UUID.randomUUID(), ReportCategory.SPAM, "d", "ip");
        UserReport newOne = UserReport.create(UUID.randomUUID(), UUID.randomUUID(), ReportCategory.ABUSE, "d2", "ip");
        ReflectionTestUtils.setField(oldOne, "createdAt", LocalDateTime.now().minusSeconds(1));
        ReflectionTestUtils.setField(newOne, "createdAt", LocalDateTime.now());

        given(entityService.getOpenReports(page))
            .willReturn(new PageImpl<>(List.of(newOne, oldOne), page, 2));

        // when
        Page<ReportSummaryResponse> result = reportService.getOpenReports(page);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).category()).isEqualTo(ReportCategory.ABUSE);
        assertThat(result.getContent().get(1).category()).isEqualTo(ReportCategory.SPAM);
        assertThat(result.getSort().getOrderFor("createdAt").getDirection())
            .isEqualTo(Sort.Direction.DESC);

        verify(entityService).getOpenReports(page);
    }

    @Test
    @DisplayName("getMyReport - 프로필 매핑 후, 엔티티/첨부 조회 및 DTO 매핑")
    void getMyReport_success() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID reporterPid = UUID.randomUUID();
        UUID targetPid = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();

        Profile me = mock(Profile.class);
        given(profileService.getByMemberId(memberId)).willReturn(me);
        given(me.getId()).willReturn(reporterPid);

        UserReport report = UserReport.create(reporterPid, targetPid, ReportCategory.SPAM, "detail", "ip");
        ReflectionTestUtils.setField(report, "id", reportId);
        ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.now());

        given(entityService.getById(reportId)).willReturn(report);
        given(entityService.getAttachmentUrls(reportId)).willReturn(List.of("https://cdn/reports/1.png"));

        // when
        var resp = reportService.getMyReport(memberId, reportId);

        // then
        assertThat(resp.reportId()).isEqualTo(reportId);
        assertThat(resp.reporterProfileId()).isEqualTo(reporterPid);
        assertThat(resp.targetProfileId()).isEqualTo(targetPid);
        assertThat(resp.detail()).isEqualTo("detail");
        assertThat(resp.images()).containsExactly("https://cdn/reports/1.png");
        assertThat(resp.createdAt()).isNotNull();

        verify(profileService).getByMemberId(memberId);
        verify(entityService).getById(reportId);
        verify(entityService).getAttachmentUrls(reportId);
    }

    @Test
    @DisplayName("getMyReports - 멤버→프로필 매핑 후, createdAt DESC 정렬로 조회/매핑")
    void getMyReports_success_createdAtDesc() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID reporterPid = UUID.randomUUID();

        ReportPageRequest req = new ReportPageRequest();
        req.setPage(0);
        req.setSize(5);

        Profile reporter = Mockito.mock(Profile.class);
        given(profileService.getByMemberId(memberId)).willReturn(reporter);
        given(reporter.getId()).willReturn(reporterPid);

        UserReport r1 = UserReport.create(reporterPid, UUID.randomUUID(), ReportCategory.SPAM, "d1", "ip");
        UserReport r2 = UserReport.create(reporterPid, UUID.randomUUID(), ReportCategory.ABUSE, "d2", "ip");
        // r2가 더 최신
        ReflectionTestUtils.setField(r1, "createdAt", LocalDateTime.now().minusMinutes(1));
        ReflectionTestUtils.setField(r2, "createdAt", LocalDateTime.now());

        ArgumentCaptor<PageRequest> pageCaptor = ArgumentCaptor.forClass(PageRequest.class);
        given(entityService.getMyReports(eq(reporterPid), pageCaptor.capture()))
            .willReturn(new PageImpl<>(List.of(r2, r1),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")),
                2)
            );

        // when
        var page = reportService.getMyReports(memberId, req);

        // then: 정렬 및 크기/매핑 확인
        PageRequest used = pageCaptor.getValue();
        assertThat(used.getSort().getOrderFor("createdAt").getDirection())
            .isEqualTo(Sort.Direction.DESC);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).category()).isEqualTo(ReportCategory.ABUSE);
        assertThat(page.getContent().get(1).category()).isEqualTo(ReportCategory.SPAM);

        verify(profileService).getByMemberId(memberId);
        verify(entityService).getMyReports(eq(reporterPid), any(PageRequest.class));
    }

    @Test
    @DisplayName("신고 생성 성공 - 도메인 검증 후 저장, 이미지 업로드/첨부 저장")
    void createReport_success() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID reporterPid = UUID.randomUUID();
        UUID targetPid = UUID.randomUUID();
        var form = new ReportCreateRequest(targetPid, ReportCategory.SPAM, List.of(), "도배");

        Profile reporter = mock(Profile.class);

        given(profileService.getByMemberId(memberId)).willReturn(reporter);
        given(reporter.getId()).willReturn(reporterPid);
        given(profileService.existsById(targetPid)).willReturn(true);
        given(entityService.getLastByPair(reporterPid, targetPid)).willReturn(Optional.empty());
        given(entityService.existsByReporterTargetIpAfter(eq(reporterPid), eq(targetPid), anyString(), any()))
            .willReturn(false);

        UserReport report = UserReport.create(reporterPid, targetPid, ReportCategory.SPAM, "도배", "iphash");
        // save 시 id가 세팅되었다고 가정
        willAnswer(inv -> {
            UserReport r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
            return r;
        }).given(entityService).save(any(UserReport.class));

        // when
        var resp = reportService.createReport(memberId, form, "iphash");

        // then
        assertThat(resp.reportId()).isNotNull();
        assertThat(resp.reporterProfileId()).isEqualTo(reporterPid);
        assertThat(resp.targetProfileId()).isEqualTo(targetPid);

        verify(profileService).getByMemberId(memberId);
        verify(entityService).getLastByPair(reporterPid, targetPid);
        verify(entityService).existsByReporterTargetIpAfter(eq(reporterPid), eq(targetPid), anyString(), any());
        verify(entityService).save(any(UserReport.class));
        verify(s3StorageService, never()).uploadImage(any(), anyString()); // 이미지 없음
    }

    @Test
    @DisplayName("신고 생성 실패 - 대상 프로필 없음")
    void createReport_fail_targetNotFound() {
        UUID memberId = UUID.randomUUID();
        UUID targetPid = UUID.randomUUID();
        var form = new ReportCreateRequest(targetPid, ReportCategory.ABUSE, List.of(), "욕설");

        Profile reporter = mock(Profile.class);
        given(profileService.getByMemberId(memberId)).willReturn(reporter);
        given(profileService.existsById(targetPid)).willReturn(false);

        assertThatThrownBy(() -> reportService.createReport(memberId, form, "ip"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("존재하지 않는 프로필");

        verify(entityService, never()).save(any(UserReport.class));
        verify(s3StorageService, never()).uploadImage(any(), anyString());
    }

    @Test
    @DisplayName("신고 생성 실패 - 자기신고 금지")
    void createReport_fail_self() {
        UUID memberId = UUID.randomUUID();
        UUID pid = UUID.randomUUID();
        var form = new ReportCreateRequest(pid, ReportCategory.ABUSE, List.of(), "욕설");

        Profile reporter = mock(Profile.class);
        given(profileService.getByMemberId(memberId)).willReturn(reporter);
        given(reporter.getId()).willReturn(pid);
        given(profileService.existsById(pid)).willReturn(true);

        assertThatThrownBy(() -> reportService.createReport(memberId, form, "ip"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("자기 자신을 신고");
    }

    @Test
    @DisplayName("신고 생성 실패 - 동일 사용자 쿨타임 위반")
    void createReport_fail_cooltime_pair() {
        UUID memberId = UUID.randomUUID();
        UUID reporterPid = UUID.randomUUID();
        UUID targetPid = UUID.randomUUID();
        var form = new ReportCreateRequest(targetPid, ReportCategory.SEXUAL, List.of(), "불쾌");

        Profile reporter = mock(Profile.class);
        given(profileService.getByMemberId(memberId)).willReturn(reporter);
        given(reporter.getId()).willReturn(reporterPid);
        given(profileService.existsById(targetPid)).willReturn(true);

        var last = UserReport.create(reporterPid, targetPid, ReportCategory.SEXUAL, "이전", "ip");
        ReflectionTestUtils.setField(last, "createdAt", LocalDateTime.now());
        given(entityService.getLastByPair(eq(reporterPid), eq(targetPid)))
            .willReturn(Optional.of(last));

        assertThatThrownBy(() -> reportService.createReport(memberId, form, "ip"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("6시간");
    }

    @Test
    @DisplayName("신고 생성 실패 - 동일 IP 해시 중복")
    void createReport_fail_cooltime_iphash() {
        UUID memberId = UUID.randomUUID();
        UUID reporterPid = UUID.randomUUID();
        UUID targetPid = UUID.randomUUID();
        var form = new ReportCreateRequest(targetPid, ReportCategory.ILLEGAL, List.of(), "의심");

        Profile reporter = mock(Profile.class);
        given(profileService.getByMemberId(memberId)).willReturn(reporter);
        given(reporter.getId()).willReturn(reporterPid);
        given(profileService.existsById(targetPid)).willReturn(true);

        given(entityService.getLastByPair(reporterPid, targetPid)).willReturn(Optional.empty());
        given(entityService.existsByReporterTargetIpAfter(eq(reporterPid), eq(targetPid), anyString(), any()))
            .willReturn(true);

        assertThatThrownBy(() -> reportService.createReport(memberId, form, "iphash"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("이미");
    }

    @Test
    @DisplayName("신고 생성 성공 - 이미지 2장 업로드 & 첨부 저장")
    void createReport_success_withImages() {
        UUID memberId = UUID.randomUUID();
        UUID reporterPid = UUID.randomUUID();
        UUID targetPid = UUID.randomUUID();
        var img1 = new MockMultipartFile("images","a.jpg","image/jpeg", new byte[]{1,2});
        var img2 = new MockMultipartFile("images","b.png","image/png", new byte[]{3,4});
        var form = new ReportCreateRequest(targetPid, ReportCategory.ETC, List.of(img1, img2), "기타");

        Profile reporter = mock(Profile.class);
        given(profileService.getByMemberId(memberId)).willReturn(reporter);
        given(reporter.getId()).willReturn(reporterPid);
        given(profileService.existsById(targetPid)).willReturn(true);

        given(entityService.getLastByPair(reporterPid, targetPid)).willReturn(Optional.empty());
        given(entityService.existsByReporterTargetIpAfter(eq(reporterPid), eq(targetPid), anyString(), any()))
            .willReturn(false);

        willAnswer(inv -> {
            UserReport r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
            return r;
        }).given(entityService).save(any(UserReport.class));

        given(s3StorageService.uploadImage(eq(img1), contains("/reports"))).willReturn("https://cdn/reports/1.png");
        given(s3StorageService.uploadImage(eq(img2), contains("/reports"))).willReturn("https://cdn/reports/2.png");

        var resp = reportService.createReport(memberId, form, "hash");

        assertThat(resp.images()).hasSize(2);
        verify(entityService, times(1)).saveAll(any(List.class));
    }

    @Test
    @DisplayName("삭제 성공 - 작성자 본인 + OPEN → S3 첨부 정리 후 DB 삭제")
    void deleteReport_success_ownerOpen() {
        UUID memberId = UUID.randomUUID();
        UUID callerPid = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();

        // reporter 프로필
        Profile reporter = mock(Profile.class);
        given(profileService.getByMemberId(memberId)).willReturn(reporter);
        given(reporter.getId()).willReturn(callerPid);

        // 조회된 신고 (작성자 = callerPid, 상태 OPEN)
        UserReport report = UserReport.create(callerPid, UUID.randomUUID(),
            ReportCategory.SPAM, "삭제", "ip");
        ReflectionTestUtils.setField(report, "id", reportId);
        given(entityService.getById(reportId)).willReturn(report);

        // 첨부 2개 반환 + S3 삭제 OK
        var att1 = ReportAttachment.create(reportId, "https://cdn/reports/a.png");
        var att2 = ReportAttachment.create(reportId, "https://cdn/reports/b.png");
        given(entityService.getAttachmentsByReportId(reportId)).willReturn(List.of(att1, att2));

        // when
        assertThatCode(() -> reportService.deleteReport(memberId, reportId)).doesNotThrowAnyException();

        // then
        verify(s3StorageService).deleteImage("https://cdn/reports/a.png");
        verify(s3StorageService).deleteImage("https://cdn/reports/b.png");
        verify(entityService).deleteReport(memberId, reportId);
    }

    @Test
    @DisplayName("삭제 실패 - 작성자 아님 → 예외")
    void deleteReport_fail_notOwner() {
        UUID memberId = UUID.randomUUID();
        UUID callerPid = UUID.randomUUID();
        UUID ownerPid = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();

        Profile caller = mock(Profile.class);
        given(profileService.getByMemberId(memberId)).willReturn(caller);
        given(caller.getId()).willReturn(callerPid);

        UserReport report = UserReport.create(ownerPid, UUID.randomUUID(),
            ReportCategory.ABUSE, "신고", "ip");
        ReflectionTestUtils.setField(report, "id", reportId);
        given(entityService.getById(reportId)).willReturn(report);

        assertThatThrownBy(() -> reportService.deleteReport(memberId, reportId))
            .isInstanceOf(IllegalStateException.class);

        verify(entityService, never()).deleteReport(any(), any());
    }

    @Test
    @DisplayName("삭제 실패 - 상태 ENDED → 예외")
    void deleteReport_fail_notOpen() {
        UUID memberId = UUID.randomUUID();
        UUID callerPid = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        Profile caller = mock(Profile.class);
        given(profileService.getByMemberId(memberId)).willReturn(caller);
        given(caller.getId()).willReturn(callerPid);

        UserReport report = UserReport.create(callerPid, UUID.randomUUID(),
            ReportCategory.ETC, "신고", "ip");
        ReflectionTestUtils.setField(report, "id", reportId);
        report.processReport(adminId, "삭제 테스트");
        given(entityService.getById(reportId)).willReturn(report);

        assertThatThrownBy(() -> reportService.deleteReport(memberId, reportId))
            .isInstanceOf(IllegalStateException.class);

        verify(entityService, never()).deleteReport(any(), any());
    }

    @Test
    @DisplayName("processReport - OPEN→ENDED 전이 및 상세 매핑")
    void processReport_success() {
        UUID adminMemberId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();

        // reporter/target은 임의
        UserReport report = UserReport.create(UUID.randomUUID(), UUID.randomUUID(),
            ReportCategory.ABUSE, "d", "ip");
        ReflectionTestUtils.setField(report, "id", reportId);
        ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.now());

        given(entityService.getById(reportId)).willReturn(report);
        // 처리 수행 시 엔티티 변경되도록 스텁
        willAnswer(inv -> {
            ReflectionTestUtils.setField(report, "status", ReportStatus.ENDED);
            ReflectionTestUtils.setField(report, "adminReply", "메모");
            ReflectionTestUtils.setField(report, "processedBy", adminMemberId);
            ReflectionTestUtils.setField(report, "processedAt", LocalDateTime.now());
            return null;
        }).given(entityService).processReport(eq(report), eq(adminMemberId), eq("메모"));
        given(entityService.getAttachmentUrls(reportId)).willReturn(List.of("https://cdn/x.png"));

        var resp = reportService.processReport(adminMemberId, reportId, new ReportProcessRequest("메모"));

        assertThat(resp.status()).isEqualTo(ReportStatus.ENDED);
        assertThat(resp.images()).containsExactly("https://cdn/x.png");
        assertThat(resp.adminReply()).isIn(null, "메모");
        assertThat(resp.processedAt()).isNotNull();

        verify(entityService).getById(reportId);
        verify(entityService).processReport(eq(report), eq(adminMemberId), eq("메모"));
        verify(entityService).getAttachmentUrls(reportId);
    }

    @Test
    @DisplayName("processReport - ENDED 상태면 409")
    void processReport_conflict() {
        UUID adminMemberId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();

        UserReport report = UserReport.create(UUID.randomUUID(), UUID.randomUUID(),
            ReportCategory.SPAM, "d", "ip");
        report.processReport(adminMemberId, "x");

        given(entityService.getById(reportId)).willReturn(report);

        assertThatThrownBy(() -> reportService.processReport(adminMemberId, reportId, new ReportProcessRequest("x")))
            .isInstanceOf(IllegalStateException.class);

        verify(entityService, never()).processReport(any(), any(), any());
    }
}
