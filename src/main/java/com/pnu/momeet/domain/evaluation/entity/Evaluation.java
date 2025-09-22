package com.pnu.momeet.domain.evaluation.entity;

import com.pnu.momeet.domain.common.entity.BaseCreatedEntity;
import com.pnu.momeet.domain.evaluation.enums.Rating;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "evaluation",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_evaluation", columnNames = {"meetup_id", "evaluator_profile_id", "target_profile_id", "ip_hash"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Evaluation extends BaseCreatedEntity {

    @Column(name = "meetup_id", nullable = false)
    private UUID meetupId;

    @Column(name = "evaluator_profile_id", nullable = false)
    private UUID evaluatorProfileId;

    @Column(name = "target_profile_id", nullable = false)
    private UUID targetProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rating", nullable = false)
    private Rating rating;

    @Column(name = "ip_hash", nullable = false, length = 128)
    private String ipHash;

    public static Evaluation create(
        UUID meetupId,
        UUID evaluatorProfileId,
        UUID targetProfileId,
        Rating rating,
        String ipHash
    ) {
        return new Evaluation(meetupId, evaluatorProfileId, targetProfileId, rating, ipHash);
    }
}
