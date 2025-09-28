package com.pnu.momeet.unit.badge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.common.event.CoreEventPublisher;
import com.pnu.momeet.common.event.DomainEvent;
import com.pnu.momeet.common.logging.Source;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import com.pnu.momeet.domain.evaluation.event.EvaluationEventPublisher;
import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EvaluationEventPublisherTest {

    @Mock
    CoreEventPublisher core;

    @InjectMocks
    EvaluationEventPublisher publisher;

    @DisplayName("EvaluationEventPublisher.publishSubmitted → CoreEventPublisher로 올바르게 위임한다")
    @Test
    void publishSubmitted_callsCoreWithComposedEventAndKv() {
        // given
        UUID meetupId = UUID.randomUUID();
        UUID evaluatorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Rating rating = Rating.LIKE;
        Source source = Source.of(this);

        ArgumentCaptor<DomainEvent> eventCap = ArgumentCaptor.forClass(DomainEvent.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> kvCap = ArgumentCaptor.forClass(Map.class);

        // when
        publisher.publishSubmitted(meetupId, evaluatorId, targetId, rating, source);

        // then
        verify(core).publish(eventCap.capture(), eq(source), kvCap.capture());

        DomainEvent event = eventCap.getValue();
        Map<String, Object> kv = kvCap.getValue();

        assertThat(event).isInstanceOf(EvaluationSubmittedEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.type()).isEqualTo("EvaluationSubmittedEvent");

        assertThat(kv)
            .containsKeys("eventId", "meetupId", "evaluatorProfileId", "targetProfileId", "rating")
            .containsEntry("meetupId", meetupId)
            .containsEntry("evaluatorProfileId", evaluatorId)
            .containsEntry("targetProfileId", targetId);
    }
}