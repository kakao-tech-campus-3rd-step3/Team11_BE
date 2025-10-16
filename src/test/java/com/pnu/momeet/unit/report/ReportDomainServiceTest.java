package com.pnu.momeet.unit.report;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.pnu.momeet.domain.report.entity.ReportAttachment;
import com.pnu.momeet.domain.report.entity.UserReport;
import com.pnu.momeet.domain.report.enums.ReportCategory;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        verify(entityService, times(2)).save(any(ReportAttachment.class));
    }
}
