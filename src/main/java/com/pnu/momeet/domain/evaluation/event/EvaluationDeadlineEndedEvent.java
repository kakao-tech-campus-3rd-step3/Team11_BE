package com.pnu.momeet.domain.evaluation.event;

import com.pnu.momeet.common.event.DomainEvent;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
public class EvaluationDeadlineEndedEvent extends DomainEvent {
    private final UUID meetupId;

    public EvaluationDeadlineEndedEvent(UUID meetupId) {
        super();
        this.meetupId = meetupId;
    }

    @Override
    public Map<String, Object> logInfo() {
        Map<String, Object> m = super.logInfo();
        m.put("meetupId", meetupId);
        return m;
    }
}
