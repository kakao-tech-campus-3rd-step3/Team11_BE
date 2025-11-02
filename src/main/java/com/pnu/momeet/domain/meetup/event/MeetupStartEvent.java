package com.pnu.momeet.domain.meetup.event;

import com.pnu.momeet.common.event.DomainEvent;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class MeetupStartEvent extends DomainEvent {
    private final UUID meetupId;

    public MeetupStartEvent(UUID meetupId) {
        super();
        this.meetupId = meetupId;
    }

    @Override
    public Map<String, Object> logInfo() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>(super.logInfo());
        m.put("meetupId", meetupId);
        return m;
    }
}
