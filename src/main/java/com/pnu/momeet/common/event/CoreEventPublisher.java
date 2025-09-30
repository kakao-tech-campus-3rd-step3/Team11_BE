package com.pnu.momeet.common.event;

import com.pnu.momeet.common.logging.LogTags;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoreEventPublisher {

    private final ApplicationEventPublisher delegate;

    public void publish(DomainEvent event) {
        StringBuilder sb = new StringBuilder(LogTags.PUBLISH);
        for (Map.Entry<String, Object> e : event.logInfo().entrySet()) {
            sb.append(' ').append(e.getKey()).append('=').append(e.getValue());
        }
        log.info(sb.toString());
        delegate.publishEvent(event);
    }
}
