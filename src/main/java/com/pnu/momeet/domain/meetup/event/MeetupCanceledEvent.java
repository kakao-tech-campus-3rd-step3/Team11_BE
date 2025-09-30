package com.pnu.momeet.domain.meetup.event;

import com.pnu.momeet.common.event.DomainEvent;
import com.pnu.momeet.domain.member.enums.Role;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
public class MeetupCanceledEvent extends DomainEvent {
    private final UUID meetupId;
    private final Role requestedBy;

    public MeetupCanceledEvent(
        UUID meetupId,
        Role requestedBy
    ) {
        super();
        this.meetupId = meetupId;
        this.requestedBy = requestedBy;
    }

    @Override
    public Map<String, Object> logInfo() {
        Map<String, Object> m = super.logInfo();
        m.put("meetupId", meetupId);
        m.put("requestedBy", requestedBy);
        return m;
    }
}
