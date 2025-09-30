package com.pnu.momeet.domain.chatting.entity;

import com.pnu.momeet.domain.chatting.enums.ChatMessageType;
import com.pnu.momeet.domain.common.entity.SimpleCreationEntity;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.participant.entity.Participant;
import com.pnu.momeet.domain.profile.entity.Profile;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "chat_message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends SimpleCreationEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "meetup_id", nullable = false)
    private Meetup meetup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Participant sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private Profile profile;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private ChatMessageType type = ChatMessageType.TEXT;

    @NotNull
    @Column(name = "content", nullable = false, length = 1200)
    private String content;

    public ChatMessage(Participant sender, ChatMessageType type, String content) {
        this.sender = sender;
        this.type = type;
        this.content = content;
        this.meetup = sender.getMeetup();
        this.profile = sender.getProfile();
    }
}