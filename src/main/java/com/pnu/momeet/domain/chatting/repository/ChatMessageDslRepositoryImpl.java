package com.pnu.momeet.domain.chatting.repository;

import com.pnu.momeet.domain.chatting.entity.ChatMessage;
import com.pnu.momeet.domain.chatting.entity.QChatMessage;
import com.pnu.momeet.domain.common.dto.response.CursorInfo;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatMessageDslRepositoryImpl implements ChatMessageDslRepository{
    private final JPAQueryFactory jpaQueryFactory;

    public CursorInfo<ChatMessage> findHistoriesByMeetupId(UUID meetupId, int size, Long cursorId) {
        QChatMessage chatMessage = QChatMessage.chatMessage;

        BooleanExpression condition = chatMessage.meetup.id.eq(meetupId);
        if (cursorId != null) {
            condition = condition.and(chatMessage.id.loe(cursorId));
        }

        List<ChatMessage> content = jpaQueryFactory
                .selectFrom(chatMessage)
                .where(condition)
                .orderBy(chatMessage.id.desc())
                .limit(size + 1)
                .fetch();
        if (content.size() > size) {
            Long nextId = content.get(size).getId();
            content = content.subList(0, size);
            return new CursorInfo<>(content, nextId); // 다음 커서 ID 포함
        } else {
            return new CursorInfo<>(content); // 다음 커서 ID 없음
        }
    }
}
