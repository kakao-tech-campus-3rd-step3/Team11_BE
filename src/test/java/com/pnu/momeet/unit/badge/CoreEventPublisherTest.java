package com.pnu.momeet.unit.badge;

import static org.mockito.Mockito.verify;

import com.pnu.momeet.common.event.CoreEventPublisher;
import com.pnu.momeet.common.event.DomainEvent;
import com.pnu.momeet.common.logging.Source;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import com.pnu.momeet.domain.evaluation.event.EvaluationSubmittedEvent;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            Rating.LIKE, LocalDateTime.now(), UUID.randomUUID()
        );

        // when
        core.publish(event, Source.of(this), Map.of("k", "v"));

        // then
        verify(delegate).publishEvent(event);
    }
}
