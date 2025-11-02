package com.pnu.momeet.domain.participant.event;

import com.pnu.momeet.common.event.DomainEvent;
import com.pnu.momeet.domain.participant.entity.Participant;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public abstract class ParticipantEvent extends DomainEvent {
    UUID meetupId;
    Participant participant;

    public ParticipantEvent(
        UUID meetupId,
        Participant participant
    ) {
        super();
        this.meetupId = meetupId;
        this.participant = participant;
    }

    @Override
    public Map<String, Object> logInfo() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>(super.logInfo());
        m.put("meetupId", meetupId);
        m.put("participantId", participant.getId());
        return m;
    }

}
