package com.pnu.momeet.unit.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.domain.evaluation.entity.Evaluation;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import com.pnu.momeet.domain.evaluation.repository.EvaluationDslRepository;
import com.pnu.momeet.domain.evaluation.repository.EvaluationRepository;
import com.pnu.momeet.domain.evaluation.service.EvaluationEntityService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EvaluationEntityServiceTest {

    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock private EvaluationDslRepository evaluationDslRepository;

    @InjectMocks
    private EvaluationEntityService service;

    @Test
    @DisplayName("existsByMeetupAndPair - 모임 단위 evaluator→target 중복 여부 반환")
    void existsByMeetupAndPair() {
        UUID meetupId = UUID.randomUUID();
        UUID evaluatorPid = UUID.randomUUID();
        UUID targetPid = UUID.randomUUID();

        given(evaluationRepository.existsByMeetupIdAndEvaluatorProfileIdAndTargetProfileId(
            meetupId, evaluatorPid, targetPid)).willReturn(true);

        boolean exists = service.existsByMeetupAndPair(meetupId, evaluatorPid, targetPid);

        assertThat(exists).isTrue();
        verify(evaluationRepository).existsByMeetupIdAndEvaluatorProfileIdAndTargetProfileId(
            meetupId, evaluatorPid, targetPid);
    }

    @Test
    @DisplayName("getLastByPair - evaluator→target 최근 평가 조회")
    void getLastByPair() {
        UUID evaluatorPid = UUID.randomUUID();
        UUID targetPid = UUID.randomUUID();
        Evaluation last = Evaluation.create(UUID.randomUUID(), evaluatorPid, targetPid, Rating.LIKE, "hash");
        ReflectionTestUtils.setField(last, "createdAt", LocalDateTime.now());

        given(evaluationRepository.findTopByEvaluatorProfileIdAndTargetProfileIdOrderByCreatedAtDesc(
            evaluatorPid, targetPid)).willReturn(Optional.of(last));

        Optional<Evaluation> found = service.getLastByPair(evaluatorPid, targetPid);

        assertThat(found).isPresent();
        assertThat(found.get().getRating()).isEqualTo(Rating.LIKE);
    }

    @Test
    @DisplayName("existsByMeetupTargetIpAfter - 동일 IP 해시 쿨타임 내 중복 여부")
    void existsByMeetupTargetIpAfter() {
        UUID meetupId = UUID.randomUUID();
        UUID targetPid = UUID.randomUUID();
        LocalDateTime after = LocalDateTime.now().minusHours(6);

        given(evaluationRepository.existsByMeetupIdAndTargetProfileIdAndIpHashAndCreatedAtAfter(
            meetupId, targetPid, "ip", after)).willReturn(true);

        boolean exists = service.existsByMeetupTargetIpAfter(meetupId, targetPid, "ip", after);

        assertThat(exists).isTrue();
        verify(evaluationRepository).existsByMeetupIdAndTargetProfileIdAndIpHashAndCreatedAtAfter(
            meetupId, targetPid, "ip", after);
    }

    @Test
    @DisplayName("getByMeetupAndEvaluator - 특정 모임에서 evaluator가 남긴 평가 목록")
    void getByMeetupAndEvaluator() {
        UUID meetupId = UUID.randomUUID();
        UUID evaluatorPid = UUID.randomUUID();

        Evaluation e1 = Evaluation.create(meetupId, evaluatorPid, UUID.randomUUID(), Rating.LIKE, "h1");
        Evaluation e2 = Evaluation.create(meetupId, evaluatorPid, UUID.randomUUID(), Rating.DISLIKE, "h2");

        given(evaluationRepository.findByMeetupIdAndEvaluatorProfileId(meetupId, evaluatorPid))
            .willReturn(List.of(e1, e2));

        List<Evaluation> list = service.getByMeetupAndEvaluator(meetupId, evaluatorPid);

        assertThat(list).hasSize(2);
        verify(evaluationRepository).findByMeetupIdAndEvaluatorProfileId(meetupId, evaluatorPid);
    }

    @Test
    @DisplayName("save - 저장 성공 시 반환값 검증")
    void save_success() {
        UUID meetupId = UUID.randomUUID();
        UUID evaluatorPid = UUID.randomUUID();
        UUID targetPid = UUID.randomUUID();

        Evaluation toSave = Evaluation.create(meetupId, evaluatorPid, targetPid, Rating.LIKE, "ip");
        Evaluation saved = Evaluation.create(meetupId, evaluatorPid, targetPid, Rating.LIKE, "ip");
        ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());

        given(evaluationRepository.save(any(Evaluation.class))).willReturn(saved);

        Evaluation result = service.save(toSave);

        assertThat(result.getId()).isNotNull();
        verify(evaluationRepository).save(any(Evaluation.class));
    }

    @Test
    @DisplayName("deleteById - 존재 확인 후 삭제, 미존재면 예외")
    void deleteById() {
        UUID id = UUID.randomUUID();

        given(evaluationRepository.existsById(id)).willReturn(true);
        willDoNothing().given(evaluationRepository).deleteById(id);

        service.deleteById(id);
        verify(evaluationRepository).deleteById(id);

        // not exists
        UUID missing = UUID.randomUUID();
        given(evaluationRepository.existsById(missing)).willReturn(false);

        assertThatThrownBy(() -> service.deleteById(missing))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("getMeetupIdsEvaluatedBy - DslRepository 위임")
    void getMeetupIdsEvaluatedBy() {
        UUID evaluatorId = UUID.randomUUID();
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        given(evaluationDslRepository.findEvaluatedMeetupIds(evaluatorId, ids))
            .willReturn(Set.copyOf(ids));

        Set<UUID> result = service.getMeetupIdsEvaluatedBy(evaluatorId, ids);

        assertThat(result).containsExactlyInAnyOrderElementsOf(ids);
        verify(evaluationDslRepository).findEvaluatedMeetupIds(evaluatorId, ids);
    }
}
