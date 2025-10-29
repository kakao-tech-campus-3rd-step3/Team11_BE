package com.pnu.momeet.domain.participant.repository;

import com.pnu.momeet.domain.participant.entity.Participant;

import java.util.List;
import java.util.UUID;

public interface ParticipantDslRepository {
    List<Participant> findAllVisibleByMeetupId(UUID meetupId, UUID viewerMemberId);
}
