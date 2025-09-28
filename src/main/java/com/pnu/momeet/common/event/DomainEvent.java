package com.pnu.momeet.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

public interface DomainEvent {
    UUID eventId();
    LocalDateTime occurredAt();      // UTC
    default String type() {    // 논리 타입명
        return getClass().getSimpleName();
    }
}
