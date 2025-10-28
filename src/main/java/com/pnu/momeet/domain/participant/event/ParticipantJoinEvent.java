package com.pnu.momeet.domain.participant.event;

import java.util.UUID;

public class ParticipantJoinEvent extends ParticipantEvent {

    public ParticipantJoinEvent(
        UUID meetupId,
        Long participantId
    ) {
        super(meetupId, participantId);
    }
}
