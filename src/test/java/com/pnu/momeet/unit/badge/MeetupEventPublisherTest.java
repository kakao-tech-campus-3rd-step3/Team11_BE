package com.pnu.momeet.unit.badge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.common.event.CoreEventPublisher;
import com.pnu.momeet.common.event.DomainEvent;
import com.pnu.momeet.common.logging.Source;
import com.pnu.momeet.domain.meetup.event.MeetupEventPublisher;
import com.pnu.momeet.domain.meetup.event.MeetupFinishedEvent;
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
class MeetupEventPublisherTest {

    @Mock
    CoreEventPublisher core;

    @InjectMocks
    MeetupEventPublisher publisher;

    @DisplayName("MeetupEventPublisher.publishFinished: 참가자 null이면 KV의 participants=0")
    @Test
    void publishFinished_nullParticipants_setsZeroInKv() {
        // given
        UUID meetupId = UUID.randomUUID();
        Source source = Source.of(this);

        ArgumentCaptor<DomainEvent> eventCap = ArgumentCaptor.forClass(DomainEvent.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> kvCap = ArgumentCaptor.forClass(Map.class);

        // when
        publisher.publishFinished(meetupId, null, source);

        // then
        verify(core).publish(eventCap.capture(), eq(source), kvCap.capture());

        DomainEvent event = eventCap.getValue();
        Map<String, Object> kv = kvCap.getValue();

        assertThat(event).isInstanceOf(MeetupFinishedEvent.class);
        assertThat(kv)
            .containsEntry("meetupId", meetupId)
            .containsEntry("participants", 0);
    }
}