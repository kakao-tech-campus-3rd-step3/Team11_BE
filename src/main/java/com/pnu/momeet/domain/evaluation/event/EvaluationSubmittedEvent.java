package com.pnu.momeet.domain.evaluation.event;

import com.pnu.momeet.common.event.DomainEvent;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;

@Getter
public final class EvaluationSubmittedEvent extends DomainEvent {

    private final UUID meetupId;
    private final UUID evaluatorProfileId;
    private final UUID targetProfileId;
    private final Rating rating;

    public EvaluationSubmittedEvent(UUID meetupId, UUID evaluatorId, UUID targetId, Rating rating) {
        super();
        this.meetupId = meetupId;
        this.evaluatorProfileId = evaluatorId;
        this.targetProfileId = targetId;
        this.rating = rating;
    }

    @Override public Map<String, Object> logInfo() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>(super.logInfo());
        m.put("meetupId", meetupId);
        m.put("evaluatorProfileId", evaluatorProfileId);
        m.put("targetProfileId", targetProfileId);
        m.put("rating", rating);
        return m;
    }
}
