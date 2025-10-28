package com.pnu.momeet.domain.chatting.enums;

public enum ChatActionType {
    ENTER,      // 채팅방 입장
    MESSAGE,    // 메시지 전송
    LEAVE,      // 채팅방 퇴장,
    JOIN,       // 모임 참여
    KICKED,     // 모임 강퇴
    EXIT,       // 모임 나가기
    STARTED,      // 모임 시작(OPEN -> IN_PROGRESS)
    NEAR_END,   // 모임 종료 임박 알림
    END,     // 모임 종료
    CANCELED,     // 모임 취소
    CANCELED_BY_ADMIN // 모임 관리자에 의한 모임 취소
}
