package com.pnu.momeet.common.event;

import com.pnu.momeet.common.logging.LogTags;
import com.pnu.momeet.common.logging.Source;
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

    public void publish(DomainEvent event, Source source, Map<String, Object> kv) {
        StringBuilder sb = new StringBuilder(LogTags.PUBLISH)
            .append(" type=").append(event.type())
            .append(" source=").append(source.name());
        kv.forEach((k, v) -> sb.append(' ').append(k).append('=').append(v));
        log.info(sb.toString());

        delegate.publishEvent(event);
    }
}
