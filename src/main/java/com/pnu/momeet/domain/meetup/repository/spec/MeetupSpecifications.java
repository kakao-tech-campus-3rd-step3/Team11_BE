package com.pnu.momeet.domain.meetup.repository.spec;

import com.pnu.momeet.domain.block.entity.UserBlock;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.participant.entity.Participant;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MeetupSpecifications {

    public static Specification<Meetup> visibleTo(UUID viewerProfileId) {
        return (root, query, cb) -> {
            // owner - viewer 차단 존재 여부
            Subquery<Integer> ownerBlocked = query.subquery(Integer.class);
            Root<UserBlock> ub1 = ownerBlocked.from(UserBlock.class);
            ownerBlocked.select(cb.literal(1))
                .where(cb.or(
                    cb.and(
                        cb.equal(ub1.get("blockerProfileId"), viewerProfileId),
                        cb.equal(ub1.get("blockedProfileId"), root.get("owner").get("id"))
                    ),
                    cb.and(
                        cb.equal(ub1.get("blockerProfileId"), root.get("owner").get("id")),
                        cb.equal(ub1.get("blockedProfileId"), viewerProfileId)
                    )
                ));

            // participant - viewer 차단 존재 여부
            Subquery<Integer> participantBlocked = query.subquery(Integer.class);
            Root<Participant> p = participantBlocked.from(Participant.class);
            Root<UserBlock> ub2 = participantBlocked.from(UserBlock.class);
            participantBlocked.select(cb.literal(1))
                .where(
                    cb.equal(p.get("meetup").get("id"), root.get("id")),
                    cb.or(
                        cb.and(
                            cb.equal(ub2.get("blockerProfileId"), viewerProfileId),
                            cb.equal(ub2.get("blockedProfileId"), p.get("profile").get("id"))
                        ),
                        cb.and(
                            cb.equal(ub2.get("blockerProfileId"), p.get("profile").get("id")),
                            cb.equal(ub2.get("blockedProfileId"), viewerProfileId)
                        )
                    )
                );

            return cb.and(
                cb.not(cb.exists(ownerBlocked)),
                cb.not(cb.exists(participantBlocked))
            );
        };
    }
}
