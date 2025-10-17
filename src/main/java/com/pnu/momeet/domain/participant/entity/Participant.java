package com.pnu.momeet.domain.participant.entity;

import com.pnu.momeet.domain.common.entity.SimpleCreationEntity;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.participant.enums.MeetupRole;
import com.pnu.momeet.domain.profile.entity.Profile;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "meetup_participant")
@NoArgsConstructor
@NamedEntityGraph(
    name = "Participant.withProfile",
    attributeNodes = @NamedAttributeNode("profile")
)
public class Participant extends SimpleCreationEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meetup_id", nullable = false)
    private Meetup meetup;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @NotNull
    @ColumnDefault("'MEMBER'")
    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MeetupRole role = MeetupRole.MEMBER;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_rated", nullable = false)
    private Boolean isRated = false;

    @NotNull
    @ColumnDefault("false")
    private Boolean isFinished = false;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Builder
    public Participant(
            Meetup meetup,
            Profile profile,
            MeetupRole role,
            Boolean isActive,
            Boolean isRated,
            LocalDateTime lastActiveAt
    ) {
        this.meetup = meetup;
        this.profile = profile;
        this.role = (role != null) ? role : MeetupRole.MEMBER;
        this.isActive = (isActive != null) ? isActive : false;
        this.isRated = (isRated != null) ? isRated : false;
        this.lastActiveAt = (lastActiveAt != null) ? lastActiveAt : LocalDateTime.now();
    }
}