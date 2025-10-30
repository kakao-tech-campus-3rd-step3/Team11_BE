package com.pnu.momeet.domain.chatting.enums;

public enum ChatActionType {
    // 모임 참여 관련
    JOIN,       // 모임 참여
    EXIT,       // 모임 나가기
    KICKED,     // 모임 강퇴

    // 채팅 관련
    ENTER,      // 채팅방 입장
    LEAVE,      // 채팅방 퇴장,
    MESSAGE,    // 메시지 전송

    // 모임 상태 관련
    MODIFIED,   // 모임 정보 수정
    NEAR_STARTED, // 모임 시작 임박 알림
    STARTED,      // 모임 시작(OPEN -> IN_PROGRESS)
    CANCELED,     // 모임 취소
    NEAR_END,   // 모임 종료 임박 알림
    END     // 모임 종료
}
