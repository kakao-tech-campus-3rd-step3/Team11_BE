package com.pnu.momeet.unit.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.common.event.CoreEventPublisher;
import com.pnu.momeet.domain.evaluation.dto.request.EvaluationCreateBatchRequest;
import com.pnu.momeet.domain.evaluation.dto.request.EvaluationCreateRequest;
import com.pnu.momeet.domain.evaluation.dto.response.EvaluationCreateBatchResponse;
import com.pnu.momeet.domain.evaluation.dto.response.EvaluationResponse;
import com.pnu.momeet.domain.evaluation.entity.Evaluation;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import com.pnu.momeet.domain.evaluation.service.EvaluationDomainService;
import com.pnu.momeet.domain.evaluation.service.EvaluationEntityService;
import com.pnu.momeet.domain.meetup.dto.request.MeetupSummaryPageRequest;
import com.pnu.momeet.domain.meetup.dto.response.MeetupSummaryResponse;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.enums.MainCategory;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import com.pnu.momeet.domain.meetup.service.MeetupEntityService;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.participant.enums.MeetupRole;
import com.pnu.momeet.domain.participant.service.ParticipantEntityService;
import com.pnu.momeet.domain.profile.dto.response.EvaluatableProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import com.pnu.momeet.domain.sigungu.service.SigunguEntityService;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EvaluationDomainServiceTest {

    @Mock
    private EvaluationEntityService entityService;

    @Mock
    private ProfileEntityService profileService;

    @Mock
    private ParticipantEntityService participantService;

    @Mock
    private MeetupEntityService meetupService;

    @Mock
    private SigunguEntityService sigunguService;

    @Mock
    private CoreEventPublisher coreEventPublisher;

    // 배치 테스트에서 createEvaluation 만 스텁하기 위해 Spy 사용
    @Spy
    @InjectMocks
    private EvaluationDomainService service;

    // 공통 픽스처
    private UUID memberId;
    private UUID evaluatorPid;
    private UUID targetPid;
    private UUID meetupId;
    private Profile evaluator;
    private Profile target;
    private Meetup meetup;

    private Sigungu newSigungu(Long id, String sidoName, String sigunguName) {
        try {
            Constructor<Sigungu> ctor = Sigungu.class.getDeclaredConstructor();
            ctor.setAccessible(true);                 // protected 생성자 접근
            Sigungu s = ctor.newInstance();

            // 필요한 최소 필드만 세팅
            ReflectionTestUtils.setField(s, "id", id);
            ReflectionTestUtils.setField(s, "sidoName", sidoName);
            ReflectionTestUtils.setField(s, "sigunguName", sigunguName);
            // 나머지는 테스트에 필요할 때만 추가 세팅

            return s;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        evaluatorPid = UUID.randomUUID();
        targetPid = UUID.randomUUID();
        meetupId = UUID.randomUUID();

        final Sigungu sgg = sigunguService.getById(26410L);
        evaluator = Profile.create(memberId, "평가자", 25, Gender.MALE, null, "소개", sgg);
        target = Profile.create(UUID.randomUUID(), "대상자", 27, Gender.FEMALE, null, "소개2", sgg);
        ReflectionTestUtils.setField(evaluator, "id", evaluatorPid);
        ReflectionTestUtils.setField(target, "id", targetPid);

        meetup = Meetup.builder()
            .owner(evaluator)
            .name("종료모임")
            .category(MainCategory.SPORTS)
            .description("desc")
            .capacity(10)
            .scoreLimit(3.0)
            .locationPoint(null)
            .address("어딘가")
            .sigungu(sgg)
            .endAt(LocalDateTime.now().minusDays(1))
            .build();
        ReflectionTestUtils.setField(meetup, "id", meetupId);
        ReflectionTestUtils.setField(meetup, "status", MeetupStatus.ENDED);
    }

    private EvaluationResponse resp(UUID target, Rating rating) {
        return new EvaluationResponse(
            UUID.randomUUID(), meetupId, evaluatorPid, target, rating.name(), LocalDateTime.now()
        );
    }

    private void commonBatchStubs(UUID... participantProfileIds) {
        // 평가자 로드
        given(profileService.getByMemberId(memberId)).willReturn(evaluator);
        final Sigungu sgg = evaluator.getBaseLocation();
        // 참가자 목록 (자기 자신 포함 후 서비스에서 제외)
        List<Participant> parts = new ArrayList<>();
        parts.add(Participant.builder().profile(evaluator).role(MeetupRole.MEMBER).meetup(meetup).build());
        for (UUID pid : participantProfileIds) {
            Profile p = Profile.create(UUID.randomUUID(), "p", 20, Gender.MALE, null, "", sgg);
            ReflectionTestUtils.setField(p, "id", pid);
            parts.add(Participant.builder().profile(p).role(MeetupRole.MEMBER).meetup(meetup).build());
        }
        given(participantService.getAllByMeetupId(meetupId)).willReturn(parts);
        // 기존 내가 남긴 평가들
        given(entityService.getByMeetupAndEvaluator(meetupId, evaluatorPid)).willReturn(List.of());
    }

    @Test
    @DisplayName("배치 성공: 두 대상 모두 생성 → created=2, already/invalid=0")
    void batch_success_twoCreated() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        commonBatchStubs(a, b);

        // 단건 생성 성공 스텁
        doReturn(resp(a, Rating.LIKE))
            .when(service).createEvaluation(
                eq(meetupId), eq(memberId),
                argThat(r -> r.targetProfileId().equals(a) && r.rating() == Rating.LIKE),
                anyString()
            );

        doReturn(resp(b, Rating.DISLIKE))
            .when(service).createEvaluation(
                eq(meetupId), eq(memberId),
                argThat(r -> r.targetProfileId().equals(b) && r.rating() == Rating.DISLIKE),
                anyString()
            );

        var req = new EvaluationCreateBatchRequest(List.of(
            new EvaluationCreateRequest(a, Rating.LIKE),
            new EvaluationCreateRequest(b, Rating.DISLIKE)
        ));

        EvaluationCreateBatchResponse result = service.createEvaluations(memberId, meetupId, req, "ip#");

        assertThat(result.created()).hasSize(2);
        assertThat(result.alreadyEvaluated()).isEmpty();
        assertThat(result.invalid()).isEmpty();
    }

    @Test
    @DisplayName("배치: 이미 평가한 대상은 created가 아닌 alreadyEvaluated로 분류")
    void batch_alreadyEvaluated_isCollected() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        commonBatchStubs(a, b);

        // a는 기존 평가 존재
        var existing = Evaluation.create(meetupId, evaluatorPid, a, Rating.LIKE, "old");
        given(entityService.getByMeetupAndEvaluator(meetupId, evaluatorPid)).willReturn(List.of(existing));

        // b는 생성
        doReturn(resp(b, Rating.DISLIKE))
            .when(service).createEvaluation(eq(meetupId), eq(memberId), any(EvaluationCreateRequest.class), anyString());

        var req = new EvaluationCreateBatchRequest(List.of(
            new EvaluationCreateRequest(a, Rating.LIKE),
            new EvaluationCreateRequest(b, Rating.DISLIKE)
        ));

        EvaluationCreateBatchResponse result = service.createEvaluations(memberId, meetupId, req, "ip#");

        assertThat(result.created()).extracting(EvaluationResponse::targetProfileId).containsExactly(b);
        assertThat(result.alreadyEvaluated()).containsExactly(a);
        assertThat(result.invalid()).isEmpty();
    }

    @Test
    @DisplayName("배치: 요청 내 중복 대상 → invalid에 수집 (부분 성공 유지)")
    void batch_duplicateInRequest_isInvalid() {
        UUID a = UUID.randomUUID();
        commonBatchStubs(a);

        // 첫 항목은 성공하도록 스텁
        doReturn(resp(a, Rating.LIKE))
            .when(service).createEvaluation(eq(meetupId), eq(memberId), any(EvaluationCreateRequest.class), anyString());

        var req = new EvaluationCreateBatchRequest(List.of(
            new EvaluationCreateRequest(a, Rating.LIKE),
            new EvaluationCreateRequest(a, Rating.DISLIKE) // 중복
        ));

        EvaluationCreateBatchResponse result = service.createEvaluations(memberId, meetupId, req, "ip#");

        assertThat(result.created().size()).isBetween(0, 1);
        assertThat(result.invalid()).hasSize(1);
        assertThat(result.invalid().getFirst().targetProfileId()).isEqualTo(a);
        assertThat(result.invalid().getFirst().message()).contains("중복");
    }

    @Test
    @DisplayName("배치: 비참가자 대상 → invalid에 수집")
    void batch_notParticipant_isInvalid() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        // 참가자에는 a만 포함(= b는 비참가자)
        commonBatchStubs(a);

        // a는 성공
        doReturn(resp(a, Rating.LIKE))
            .when(service).createEvaluation(eq(meetupId), eq(memberId), any(EvaluationCreateRequest.class), anyString());

        var req = new EvaluationCreateBatchRequest(List.of(
            new EvaluationCreateRequest(a, Rating.LIKE),    // 참가자
            new EvaluationCreateRequest(b, Rating.DISLIKE)  // 참가자가 아님
        ));

        EvaluationCreateBatchResponse result = service.createEvaluations(memberId, meetupId, req, "ip#");

        assertThat(result.created()).extracting(EvaluationResponse::targetProfileId).containsExactly(a);
        assertThat(result.invalid()).hasSize(1);
        assertThat(result.invalid().getFirst().targetProfileId()).isEqualTo(b);
        assertThat(result.invalid().getFirst().message()).contains("모임 참가자가 아닙니다");
    }

    @Test
    @DisplayName("배치: 단건 생성에서 예외 발생 → invalid에 코드/메시지 축적")
    void batch_singleCreationThrows_isCollectedAsInvalid() {
        UUID a = UUID.randomUUID();
        commonBatchStubs(a);

        doThrow(new IllegalStateException("쿨타임 위반"))
            .when(service).createEvaluation(eq(meetupId), eq(memberId), any(EvaluationCreateRequest.class), anyString());

        var req = new EvaluationCreateBatchRequest(List.of(
            new EvaluationCreateRequest(a, Rating.LIKE)
        ));

        EvaluationCreateBatchResponse result = service.createEvaluations(memberId, meetupId, req, "ip#");

        assertThat(result.created()).isEmpty();
        assertThat(result.invalid()).hasSize(1);
        assertThat(result.invalid().getFirst().targetProfileId()).isEqualTo(a);
        assertThat(result.invalid().getFirst().message()).contains("쿨타임");
    }

    @Test
    @DisplayName("배치: 혼합 케이스 created 1, already 1, invalid 1")
    void batch_mixed_allBuckets() {
        UUID a = UUID.randomUUID(); // already
        UUID b = UUID.randomUUID(); // created
        commonBatchStubs(a, b);

        // a는 기존 평가
        var existing = Evaluation.create(meetupId, evaluatorPid, a, Rating.LIKE, "old");
        given(entityService.getByMeetupAndEvaluator(meetupId, evaluatorPid)).willReturn(List.of(existing));

        // b는 생성
        doReturn(resp(b, Rating.DISLIKE))
            .when(service).createEvaluation(eq(meetupId), eq(memberId), any(EvaluationCreateRequest.class), anyString());

        var req = new EvaluationCreateBatchRequest(List.of(
            new EvaluationCreateRequest(a, Rating.LIKE),     // already
            new EvaluationCreateRequest(b, Rating.DISLIKE),  // created
            new EvaluationCreateRequest(b, Rating.DISLIKE)   // duplicate → invalid
        ));

        EvaluationCreateBatchResponse result = service.createEvaluations(memberId, meetupId, req, "ip#");

        assertThat(result.created()).extracting(EvaluationResponse::targetProfileId).containsExactly(b);
        assertThat(result.alreadyEvaluated()).containsExactly(a);
        assertThat(result.invalid()).hasSize(1);
        assertThat(result.invalid().getFirst().targetProfileId()).isEqualTo(b);
        assertThat(result.invalid().getFirst().message()).contains("중복");
    }

    @Test
    @DisplayName("createEvaluation 성공 - LIKE")
    void createEvaluation_success_like() {
        // given
        given(profileService.getByMemberId(memberId)).willReturn(evaluator);
        given(profileService.getById(targetPid)).willReturn(target);
        given(participantService.existsByProfileIdAndMeetupId(evaluatorPid, meetupId)).willReturn(true);
        given(participantService.existsByProfileIdAndMeetupId(targetPid,    meetupId)).willReturn(true);

        given(entityService.existsByMeetupAndPair(meetupId, evaluatorPid, targetPid)).willReturn(false);
        given(entityService.getLastByPair(evaluatorPid, targetPid)).willReturn(Optional.empty());
        given(entityService.existsByMeetupTargetIpAfter(eq(meetupId), eq(targetPid), eq("ipHash"), any())).willReturn(false);

        Evaluation toSave = Evaluation.create(meetupId, evaluatorPid, targetPid, Rating.LIKE, "ipHash");
        Evaluation saved = Evaluation.create(meetupId, evaluatorPid, targetPid, Rating.LIKE, "ipHash");
        ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
        given(entityService.save(any(Evaluation.class))).willReturn(saved);

        // when
        EvaluationCreateRequest req = new EvaluationCreateRequest(targetPid, Rating.LIKE);
        EvaluationResponse resp = service.createEvaluation(meetupId, memberId, req, "ipHash");

        // then
        assertThat(resp.meetupId()).isEqualTo(meetupId);
        assertThat(resp.targetProfileId()).isEqualTo(targetPid);
        assertThat(resp.rating()).isEqualTo("LIKE");
        verify(coreEventPublisher).publish(any());
        verify(entityService).save(any(Evaluation.class));
    }

    @Test
    @DisplayName("createEvaluation 실패 - 자기 자신 평가 금지")
    void createEvaluation_fail_self() {
        // given
        given(profileService.getByMemberId(memberId)).willReturn(evaluator);
        given(profileService.getById(evaluatorPid)).willReturn(evaluator); // target == evaluator

        // when
        EvaluationCreateRequest req = new EvaluationCreateRequest(evaluatorPid, Rating.DISLIKE);

        // then
        assertThatThrownBy(() -> service.createEvaluation(meetupId, memberId, req, "ipHash"))
            .isInstanceOf(IllegalArgumentException.class);
        verify(entityService, never()).save(any());
    }

    @Test
    @DisplayName("createEvaluation 실패 - 동일 모임에서 evaluator→target 중복")
    void createEvaluation_fail_duplicate_in_meetup() {
        // given
        given(profileService.getByMemberId(memberId)).willReturn(evaluator);
        given(profileService.getById(targetPid)).willReturn(target);
        given(participantService.existsByProfileIdAndMeetupId(evaluatorPid, meetupId)).willReturn(true);

        EvaluationCreateRequest req = new EvaluationCreateRequest(targetPid, Rating.LIKE);

        // then
        assertThatThrownBy(() -> service.createEvaluation(meetupId, memberId, req, "ipHash"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("createEvaluation 실패 - evaluator→target 쿨타임 위반")
    void createEvaluation_fail_cooltime() {
        // given
        given(profileService.getByMemberId(memberId)).willReturn(evaluator);
        given(profileService.getById(targetPid)).willReturn(target);
        given(participantService.existsByProfileIdAndMeetupId(evaluatorPid, meetupId)).willReturn(true);

        Evaluation last = Evaluation.create(meetupId, evaluatorPid, targetPid, Rating.LIKE, "prev");
        ReflectionTestUtils.setField(last, "createdAt", LocalDateTime.now()); // 지금으로 설정 → 쿨타임 위반

        EvaluationCreateRequest req = new EvaluationCreateRequest(targetPid, Rating.DISLIKE);

        // then
        assertThatThrownBy(() -> service.createEvaluation(meetupId, memberId, req, "ipHash"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("createEvaluation 실패 - 동일 IP 해시로 타겟 재평가")
    void createEvaluation_fail_same_ip_hash() {
        // given
        given(profileService.getByMemberId(memberId)).willReturn(evaluator);
        given(profileService.getById(targetPid)).willReturn(target);
        given(participantService.existsByProfileIdAndMeetupId(evaluatorPid, meetupId)).willReturn(true);

        EvaluationCreateRequest req = new EvaluationCreateRequest(targetPid, Rating.LIKE);

        // then
        assertThatThrownBy(() -> service.createEvaluation(meetupId, memberId, req, "ipHash"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("getMyRecentMeetupsMixed 성공 - 이번 페이지 id 기준 evaluated 플래그 매핑")
    void getMyRecentMeetupsMixed_success() {
        // given
        ReflectionTestUtils.setField(meetup, "participantCount", 3);
        given(profileService.getByMemberId(memberId)).willReturn(evaluator);

        PageRequest pr = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("endAt")));
        Page<Meetup> page = new PageImpl<>(List.of(meetup), pr, 1);

        given(meetupService.getEndedMeetupsByProfileId(evaluatorPid, pr)).willReturn(page);
        // 이번 페이지에서 내가 평가한 모임 id = meetupId 1개
        given(entityService.getMeetupIdsEvaluatedBy(evaluatorPid, List.of(meetupId)))
            .willReturn(Set.of(meetupId));

        // when
        MeetupSummaryPageRequest req = new MeetupSummaryPageRequest();
        Page<MeetupSummaryResponse> result = service.getMyRecentMeetupsMixed(memberId, req);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().evaluated()).isTrue();
        verify(entityService).getMeetupIdsEvaluatedBy(eq(evaluatorPid), anyList());
    }

    @Test
    @DisplayName("getMyEvaluatedMeetups 성공 - 모두 evaluated=true")
    void getMyEvaluatedMeetups_success() {
        given(profileService.getByMemberId(memberId)).willReturn(evaluator);
        PageRequest pr = PageRequest.of(0, 10, Sort.by("endAt").descending());
        Page<Meetup> page = new PageImpl<>(List.of(meetup), pr, 1);
        given(meetupService.getEndedMeetupsByProfileIdAndEvaluated(evaluatorPid, true, pr))
            .willReturn(page);

        MeetupSummaryPageRequest req = new MeetupSummaryPageRequest();
        Page<MeetupSummaryResponse> result = service.getMyEvaluatedMeetups(memberId, req);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().evaluated()).isTrue();
    }

    @Test
    @DisplayName("getMyUnEvaluatedMeetups 성공 - 모두 evaluated=false")
    void getMyUnEvaluatedMeetups_success() {
        given(profileService.getByMemberId(memberId)).willReturn(evaluator);
        PageRequest pr = PageRequest.of(0, 10, Sort.by("endAt").descending());
        Page<Meetup> page = new PageImpl<>(List.of(meetup), pr, 1);
        given(meetupService.getEndedMeetupsByProfileIdAndEvaluated(evaluatorPid, false, pr)).willReturn(page);

        MeetupSummaryPageRequest req = new MeetupSummaryPageRequest();
        Page<MeetupSummaryResponse> result = service.getMyUnEvaluatedMeetups(memberId, req);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().evaluated()).isFalse();
    }

    @Test
    @DisplayName("getEvaluatableUsers 성공 - 자기 자신 제외 + currentEvaluation=null")
    void getEvaluatableUsers_success() {
        // given
        given(profileService.getByMemberId(memberId)).willReturn(evaluator);
        given(meetupService.getById(meetupId)).willReturn(meetup);

        Participant meP = Participant.builder().meetup(meetup).profile(evaluator).role(MeetupRole.MEMBER).build();
        Participant tgP = Participant.builder().meetup(meetup).profile(target).role(MeetupRole.MEMBER).build();
        given(participantService.getAllByMeetupId(meetupId)).willReturn(List.of(meP, tgP));

        // 동일 대상 24h 기록 없음 → 후보 포함
        given(entityService.getLastByPair(evaluatorPid, targetPid)).willReturn(Optional.empty());

        // when
        List<EvaluatableProfileResponse> list = service.getEvaluatableUsers(meetupId, memberId);

        // then
        assertThat(list).hasSize(1);
        EvaluatableProfileResponse one = list.getFirst();
        assertThat(one.profileId()).isEqualTo(targetPid);
        assertThat(one.currentEvaluation()).isNull();
    }

    @Test
    @DisplayName("getEvaluatableUsers - evaluator→target 최근 24h 기록이 있으면 제외")
    void getEvaluatableUsers_excludesWithin24h() {
        // given
        given(profileService.getByMemberId(memberId)).willReturn(evaluator);
        given(meetupService.getById(meetupId)).willReturn(meetup);
        Participant meP = Participant.builder().meetup(meetup).profile(evaluator).role(MeetupRole.MEMBER).build();
        Participant tgP = Participant.builder().meetup(meetup).profile(target).role(MeetupRole.MEMBER).build();
        given(participantService.getAllByMeetupId(meetupId)).willReturn(List.of(meP, tgP));

        // 최근 기록(now) 세팅 → 24h 위반
        Evaluation last = Evaluation.create(meetupId, evaluatorPid, targetPid, Rating.LIKE, "h");
        ReflectionTestUtils.setField(last, "createdAt", LocalDateTime.now());
        given(entityService.getLastByPair(evaluatorPid, targetPid)).willReturn(Optional.of(last));

        // when
        List<EvaluatableProfileResponse> list = service.getEvaluatableUsers(meetupId, memberId);

        // then
        assertThat(list).isEmpty();
    }

    @Test
    @DisplayName("getEvaluatableUsers - 상태가 ENDED가 아니면 빈 목록을 반환한다")
    void getEvaluatableUsers_returnsEmpty_whenStatusNotEnded() {
        // given
        given(profileService.getByMemberId(memberId)).willReturn(evaluator);
        given(meetupService.getById(meetupId)).willReturn(meetup);

        // 모임 상태를 ENDED가 아닌 값으로 설정
        ReflectionTestUtils.setField(meetup, "status", MeetupStatus.IN_PROGRESS);

        // when
        List<EvaluatableProfileResponse> list = service.getEvaluatableUsers(meetupId, memberId);

        // then
        assertThat(list).isEmpty();
    }
}
