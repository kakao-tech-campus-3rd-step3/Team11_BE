package com.pnu.momeet.domain.meetup.event;

import com.pnu.momeet.common.event.DomainEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.pnu.momeet.domain.member.enums.Role;
import lombok.Getter;

@Getter
public final class MeetupFinishedEvent extends DomainEvent {

    private final UUID meetupId;
    private final List<UUID> participantProfileIds;
    private final Role requestedBy;

    public MeetupFinishedEvent(
        UUID meetupId,
        List<UUID> participantProfileIds,
        Role requestedBy
    ) {
        super();
        this.meetupId = meetupId;
        this.participantProfileIds = participantProfileIds;
        this.requestedBy = requestedBy;
    }

    @Override
    public Map<String, Object> logInfo() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>(super.logInfo());
        m.put("meetupId", meetupId);
        m.put("participantProfileIds", participantProfileIds);
        m.put("requestedBy", requestedBy);
        return m;
    }
}
