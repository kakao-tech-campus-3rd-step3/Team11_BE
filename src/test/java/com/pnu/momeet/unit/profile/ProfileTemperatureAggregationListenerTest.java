package com.pnu.momeet.unit.profile;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.common.config.TemperatureProperties;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.service.ProfileEntityService;
import com.pnu.momeet.domain.profile.service.listener.ProfileTemperatureAggregationListener;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileTemperatureAggregationListenerTest {

    @Mock
    ProfileEntityService profileEntityService;

    @Mock
    TemperatureProperties temperatureProperties;

    @Mock
    Profile profile;

    @InjectMocks
    ProfileTemperatureAggregationListener listener;

    @Test
    @DisplayName("LIKE 평가 이벤트 처리 시: 타깃 프로필 likes 증가 및 재계산 호출")
    void onEvaluationSubmitted_like_increaseLikesAndRecalc() {
        // given
        UUID meetupId = UUID.randomUUID();
        UUID evaluator = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        EvaluationSubmittedEvent e = new EvaluationSubmittedEvent(
            meetupId, evaluator, target, Rating.LIKE
        );

        given(profileEntityService.getByIdForUpdate(target)).willReturn(profile);
        given(temperatureProperties.priorK()).willReturn(5.0);

        // when
        listener.onEvaluationSubmitted(e);

        // then
        verify(profileEntityService).getByIdForUpdate(target);
        verify(temperatureProperties).priorK();
        verify(profile).increaseLikesAndRecalc(5.0);
        verify(profile, never()).increaseDislikesAndRecalc(anyDouble());
    }

    @Test
    @DisplayName("DISLIKE 평가 이벤트 처리 시: 타깃 프로필 dislikes 증가 및 재계산 호출")
    void onEvaluationSubmitted_dislike_increaseDislikesAndRecalc() {
        // given
        UUID meetupId = UUID.randomUUID();
        UUID evaluator = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        EvaluationSubmittedEvent e = new EvaluationSubmittedEvent(
            meetupId, evaluator, target, Rating.DISLIKE
        );

        given(profileEntityService.getByIdForUpdate(target)).willReturn(profile);
        given(temperatureProperties.priorK()).willReturn(5.0);

        // when
        listener.onEvaluationSubmitted(e);

        // then
        verify(profileEntityService).getByIdForUpdate(target);
        verify(temperatureProperties).priorK();
        verify(profile).increaseDislikesAndRecalc(5.0);
        verify(profile, never()).increaseLikesAndRecalc(anyDouble());
    }
}
