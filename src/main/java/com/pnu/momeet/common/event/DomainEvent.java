package com.pnu.momeet.common.event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract class DomainEvent {
    private final UUID eventId;
    private final LocalDateTime occurredAt;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = LocalDateTime.now();
    }

    public String type() {
        return getClass().getSimpleName();
    }

    public Map<String, Object> logInfo() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        m.put("type", type());
        m.put("eventId", eventId);
        m.put("occurredAt", occurredAt);
        return m;
    }
}
