package com.pnu.momeet.domain.chatting.repository;

import com.pnu.momeet.domain.block.entity.QUserBlock;
import com.pnu.momeet.domain.chatting.entity.ChatMessage;
import com.pnu.momeet.domain.chatting.entity.QChatMessage;
import com.pnu.momeet.domain.common.dto.response.CursorInfo;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatMessageDslRepository {
    private final JPAQueryFactory jpaQueryFactory;

    public CursorInfo<ChatMessage> findHistoriesByMeetupId(UUID meetupId, UUID memberId, int size, Long cursorId) {
        QChatMessage chatMessage = QChatMessage.chatMessage;
        QUserBlock userBlock = QUserBlock.userBlock;

        BooleanExpression condition = chatMessage.meetup.id.eq(meetupId);
        if (cursorId != null) {
            condition = condition.and(chatMessage.id.lt(cursorId));
        }
        BooleanExpression viewerBlocksSender =
            userBlock.blockerId.eq(memberId).and(userBlock.blockedId.eq(chatMessage.profile.memberId));
        BooleanExpression senderBlocksViewer =
            userBlock.blockerId.eq(chatMessage.profile.memberId).and(userBlock.blockedId.eq(memberId));
        BooleanExpression notBlockedEither = JPAExpressions
            .selectOne().from(userBlock)
            .where(viewerBlocksSender.or(senderBlocksViewer))
            .notExists();

        List<ChatMessage> content = jpaQueryFactory
                .selectFrom(chatMessage)
                .where(condition.and(notBlockedEither))
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
