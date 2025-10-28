package com.pnu.momeet.domain.participant.event;

import java.util.UUID;

public class ParticipantExitEvent extends ParticipantEvent {

    public ParticipantExitEvent(
        UUID meetupId,
        Long participantId
    ) {
        super(meetupId, participantId);
    }
}
