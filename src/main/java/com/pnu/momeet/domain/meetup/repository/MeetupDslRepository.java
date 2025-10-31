package com.pnu.momeet.domain.meetup.repository;

import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.enums.MainCategory;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetupDslRepository {

    List<Meetup> findAllByDistanceAndPredicates(
            Point location,
            double radius,
            @Nullable MainCategory category,
            @Nullable String keyword,
            UUID viewerMemberId
    );

    List<Meetup> findAllByOwnerIdAndStatusIn(UUID profileId, List<MeetupStatus> statuses);

    Optional<Meetup> findParticipatedMeetupsByProfileId(UUID profileId);


    boolean existsParticipatedMeetupByProfileId(UUID profileId);

    Optional<Meetup> findByIdWithDetails(UUID meetupId);

    Page<Meetup> findEndedMeetupsByProfileId(UUID profileId, Pageable pageable);

    Page<Meetup> findEndedMeetupsByProfileIdAndEvaluated(
            UUID profileId,
            Boolean evaluated,
            Pageable pageable
    );

    boolean existsBlockedInMeetup(UUID meetupId, UUID viewerMemberId);
}
