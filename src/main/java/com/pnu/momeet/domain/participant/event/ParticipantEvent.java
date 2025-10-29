package com.pnu.momeet.domain.participant.event;

import com.pnu.momeet.common.event.DomainEvent;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public abstract class ParticipantEvent extends DomainEvent {
    UUID meetupId;
    Long participantId;

    public ParticipantEvent(
        UUID meetupId,
        Long participantId
    ) {
        super();
        this.meetupId = meetupId;
        this.participantId = participantId;
    }

    @Override
    public Map<String, Object> logInfo() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>(super.logInfo());
        m.put("meetupId", meetupId);
        m.put("participantId", participantId);
        return m;
    }

}
