package com.pnu.momeet.domain.meetup.event;

import com.pnu.momeet.common.event.DomainEvent;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;

@Getter
public final class MeetupFinishedEvent extends DomainEvent {

    private final UUID meetupId;
    private final List<UUID> participantProfileIds;

    public MeetupFinishedEvent(
        UUID meetupId,
        List<UUID> participantProfileIds
    ) {
        super();
        this.meetupId = meetupId;
        this.participantProfileIds = participantProfileIds;
    }

    @Override
    public Map<String, Object> logInfo() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>(super.logInfo());
        m.put("meetupId", meetupId);
        m.put("participantProfileIds", participantProfileIds);
        return m;
    }
}
