package com.pnu.momeet.unit.badge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.common.event.CoreEventPublisher;
import com.pnu.momeet.common.event.DomainEvent;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import com.pnu.momeet.domain.meetup.event.MeetupFinishedEvent;
import java.util.List;
import java.util.UUID;

import com.pnu.momeet.domain.member.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CoreEventPublisherTest {

    @Mock
    ApplicationEventPublisher delegate;

    @InjectMocks
    CoreEventPublisher core;

    @DisplayName("CoreEventPublisher.publish는 delegate.publishEvent로 위임한다")
    @Test
    void publish_delegatesToSpringPublisher() {
        // given
        DomainEvent event = new EvaluationSubmittedEvent(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Rating.LIKE
        );

        // when
        core.publish(event);

        // then
        verify(delegate).publishEvent(event);
    }

    @DisplayName("MeetupFinishedEvent를 그대로 위임한다")
    @Test
    void publish_meetupFinished_delegatesWithSameEvent() {
        // given
        UUID meetupId = UUID.randomUUID();
        MeetupFinishedEvent event = new MeetupFinishedEvent(meetupId, List.of(), Role.ROLE_SYSTEM);

        ArgumentCaptor<Object> cap = ArgumentCaptor.forClass(Object.class);

        // when
        core.publish(event);

        // then
        verify(delegate).publishEvent(cap.capture());
        Object forwarded = cap.getValue();
        assertThat(forwarded)
            .isInstanceOf(MeetupFinishedEvent.class)
            .isSameAs(event);
    }

    @DisplayName("EvaluationSubmittedEvent를 그대로 위임한다")
    @Test
    void publish_evaluationSubmitted_delegatesWithSameEvent() {
        // given
        DomainEvent event = new EvaluationSubmittedEvent(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Rating.LIKE
        );
        ArgumentCaptor<Object> cap = ArgumentCaptor.forClass(Object.class);

        // when
        core.publish(event);

        // then
        verify(delegate).publishEvent(cap.capture());
        Object forwarded = cap.getValue();
        assertThat(forwarded)
            .isInstanceOf(EvaluationSubmittedEvent.class)
            .isSameAs(event);
    }
}
