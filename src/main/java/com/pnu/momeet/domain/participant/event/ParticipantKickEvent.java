package com.pnu.momeet.domain.participant.event;

import com.pnu.momeet.domain.participant.entity.Participant;

import java.util.UUID;

public class ParticipantKickEvent extends ParticipantEvent {

    public ParticipantKickEvent(
        UUID meetupId,
        Participant participant
    ) {
        super(meetupId, participant);
    }
}
