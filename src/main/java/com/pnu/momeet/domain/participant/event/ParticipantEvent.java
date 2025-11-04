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
    Long participantId;
    UUID profileId;
    String nickname;
    String imageUrl;

    public ParticipantEvent(
        UUID meetupId,
        Participant participant
    ) {
        super();
        this.meetupId = meetupId;
        this.participantId = participant.getId();
        this.profileId = participant.getProfile().getId();
        this.nickname = participant.getProfile().getNickname();
        this.imageUrl = participant.getProfile().getImageUrl();
    }

    @Override
    public Map<String, Object> logInfo() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>(super.logInfo());
        m.put("meetupId", meetupId);
        m.put("participantId", participantId);
        return m;
    }

}
