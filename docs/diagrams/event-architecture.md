# 이벤트 아키텍처

## 1. 전체 이벤트 구조 개요

```mermaid
graph TB
    subgraph Publishers[이벤트 발행자]
        MSS[MeetupStateService]
        ECS[EvaluationCommandService]
        Scheduler[평가 마감 스케줄러]
    end

    subgraph Events[Domain Events]
        MFE[MeetupFinishedEvent<br/>모임 종료]
        MCE[MeetupCanceledEvent<br/>모임 취소]
        ESE[EvaluationSubmittedEvent<br/>평가 제출]
        EDE[EvaluationDeadlineEndedEvent<br/>평가 기한 만료]
    end

    subgraph Listeners[이벤트 리스너]
        BAGL[BadgeAutoGrantListener<br/>배지 자동 부여]
        CEL[ChattingEventListener<br/>채팅 처리]
        PL[ParticipantListener<br/>참가자 처리]
    end

    MSS -->|publish| MFE
    MSS -->|publish| MCE
    ECS -->|publish| ESE
    Scheduler -->|publish| EDE

    MFE -->|"@Async AFTER_COMMIT"| BAGL
    MFE -->|"@Async AFTER_COMMIT"| CEL

    MCE -->|"@Async AFTER_COMMIT"| CEL

    ESE -->|"@Async AFTER_COMMIT"| BAGL

    EDE -->|"@Order(1) BEFORE_COMMIT"| CEL
    EDE -->|"@Order(2) BEFORE_COMMIT"| PL

    style MFE fill:#e1f5ff
    style MCE fill:#ffe1e1
    style ESE fill:#e1ffe1
    style EDE fill:#fff3e1
```

---

## 2. 이벤트 상세 설명

### 2.1 MeetupFinishedEvent (모임 종료)

**발행 시점**: 모임 소유자가 모임을 종료할 때

```mermaid
sequenceDiagram
    participant MSS as MeetupStateService
    participant CEP as CoreEventPublisher
    participant EventBus as Spring Event Bus
    participant BAGL as BadgeAutoGrantListener
    participant CEL as ChattingEventListener
    participant DB as Database

    MSS->>MSS: finishMeetupInternal()
    Note over MSS: 트랜잭션 시작

    MSS->>DB: UPDATE Meetup<br/>status=ENDED, endAt=now
    MSS->>DB: UPDATE Profile<br/>completedJoinMeetups++

    MSS->>CEP: publish(MeetupFinishedEvent)
    CEP->>CEP: 로깅 (eventId, meetupId, participants)
    CEP->>EventBus: publishEvent(event)

    Note over MSS: 트랜잭션 커밋

    par 비동기 리스너 실행 (AFTER_COMMIT)
        EventBus-->>BAGL: onMeetupFinished(event)
        Note over BAGL: @Async<br/>별도 트랜잭션
        BAGL->>BAGL: 모임 완주 배지 규칙 평가
        BAGL->>DB: INSERT BadgeAward<br/>(새로운 트랜잭션)
    and
        EventBus-->>CEL: handleOnMeetupFinished(event)
        Note over CEL: @Async
        CEL->>CEL: finishMeetup(meetupId)
        CEL->>DB: WebSocket으로<br/>"모임이 종료되었습니다" 메시지
    end
```

**이벤트 데이터**:

```java
class MeetupFinishedEvent {
    UUID eventId;              // 이벤트 고유 ID
    LocalDateTime occurredAt;  // 발생 시각
    UUID meetupId;             // 종료된 모임 ID
    List<UUID> participantProfileIds;  // 참가자 프로필 ID 목록
}
```

**리스너 처리**:

| 리스너                 | 처리 내용                | 트랜잭션     | 비동기 |
| ---------------------- | ------------------------ | ------------ | ------ |
| BadgeAutoGrantListener | 모임 완주 관련 배지 부여 | REQUIRES_NEW | ✅     |
| ChattingEventListener  | 채팅방 종료 메시지 발송  | 별도         | ✅     |

---

### 2.2 MeetupCanceledEvent (모임 취소)

**발행 시점**: 관리자가 모임을 취소할 때 (사용자 취소는 이벤트 발행 없음)

```mermaid
sequenceDiagram
    participant Admin as 관리자
    participant MSC as MeetupStateController
    participant MSS as MeetupStateService
    participant CEP as CoreEventPublisher
    participant EventBus as Spring Event Bus
    participant CEL as ChattingEventListener
    participant DB as Database

    Admin->>MSC: POST /api/meetups/{id}/cancel
    MSC->>MSC: @PreAuthorize('ROLE_ADMIN')
    MSC->>MSS: cancelMeetupAdmin(meetupId)

    Note over MSS: 트랜잭션 시작

    MSS->>DB: UPDATE Meetup<br/>status=CANCELED
    MSS->>CEP: publish(MeetupCanceledEvent<br/>requestedBy=ROLE_ADMIN)
    CEP->>EventBus: publishEvent(event)

    Note over MSS: 트랜잭션 커밋

    EventBus-->>CEL: handleOnMeetupCanceled(event)
    Note over CEL: @Async AFTER_COMMIT

    CEL->>CEL: cancelByAdminMeetup(meetupId)
    CEL->>DB: WebSocket으로<br/>"관리자에 의해 모임이 취소되었습니다" 메시지
```

**이벤트 데이터**:

```java
class MeetupCanceledEvent {
    UUID eventId;
    LocalDateTime occurredAt;
    UUID meetupId;             // 취소된 모임 ID
    Role requestedBy;          // ROLE_ADMIN (관리자만 발행)
}
```

**리스너 처리**:

| 리스너                | 처리 내용                        | 트랜잭션 | 비동기 |
| --------------------- | -------------------------------- | -------- | ------ |
| ChattingEventListener | 채팅방에 관리자 취소 메시지 발송 | 별도     | ✅     |

---

### 2.3 EvaluationSubmittedEvent (평가 제출)

**발행 시점**: 참가자가 다른 참가자를 평가할 때

```mermaid
sequenceDiagram
    participant User as 사용자
    participant EC as EvaluationController
    participant ECS as EvaluationCommandService
    participant CEP as CoreEventPublisher
    participant EventBus as Spring Event Bus
    participant BAGL as BadgeAutoGrantListener
    participant BRS as BadgeRuleService
    participant BAS as BadgeAwardService
    participant DB as Database

    User->>EC: POST /api/evaluations<br/>(targetId, rating)
    EC->>ECS: submitEvaluation(evaluatorId, targetId, rating)

    Note over ECS: 트랜잭션 시작

    ECS->>DB: INSERT Evaluation
    ECS->>DB: UPDATE Profile<br/>(likes++ or dislikes++)
    ECS->>DB: UPDATE Profile<br/>(temperature 재계산)

    ECS->>CEP: publish(EvaluationSubmittedEvent)
    CEP->>EventBus: publishEvent(event)

    Note over ECS: 트랜잭션 커밋

    EventBus-->>BAGL: onEvaluationSubmitted(event)
    Note over BAGL: @Async AFTER_COMMIT<br/>REQUIRES_NEW 트랜잭션

    BAGL->>BRS: evaluateOnEvaluationSubmitted(event)
    BRS->>DB: 배지 규칙 평가<br/>(likes 개수, temperature 등)
    BRS-->>BAGL: List<badgeCode>

    loop 부여할 배지마다
        BAGL->>BAS: award(profileId, badgeCode)
        BAS->>DB: INSERT BadgeAward
    end

    BAGL-->>EventBus: 완료
```

**이벤트 데이터**:

```java
class EvaluationSubmittedEvent {
    UUID eventId;
    LocalDateTime occurredAt;
    UUID meetupId;             // 평가 대상 모임 ID
    UUID evaluatorProfileId;   // 평가자 프로필 ID
    UUID targetProfileId;      // 평가 받는 사람 프로필 ID
    Rating rating;             // LIKE or DISLIKE
}
```

**리스너 처리**:

| 리스너                 | 처리 내용                                           | 트랜잭션     | 비동기 |
| ---------------------- | --------------------------------------------------- | ------------ | ------ |
| BadgeAutoGrantListener | 평가 관련 배지 자동 부여 (좋아요 N개, 온도 달성 등) | REQUIRES_NEW | ✅     |

---

### 2.4 EvaluationDeadlineEndedEvent (평가 기한 만료)

**발행 시점**: 스케줄러가 평가 기한이 지난 모임을 감지할 때

```mermaid
sequenceDiagram
    participant Scheduler as 평가 마감 스케줄러
    participant MSS as MeetupStateService
    participant CEP as CoreEventPublisher
    participant EventBus as Spring Event Bus
    participant CEL as ChattingEventListener
    participant PL as ParticipantListener
    participant CMES as ChatMessageEntityService
    participant PES as ParticipantEntityService
    participant DB as Database

    Scheduler->>Scheduler: @Scheduled 실행<br/>(매일 자정)
    Scheduler->>DB: SELECT Meetup<br/>WHERE status=ENDED<br/>AND endAt < now() - 3일
    DB-->>Scheduler: expiredMeetups

    loop 각 만료된 모임
        Scheduler->>MSS: evaluationPeriodEnded(meetup)

        Note over MSS: 트랜잭션 시작

        MSS->>DB: UPDATE Meetup<br/>status=EVALUATION_TIMEOUT
        MSS->>CEP: publish(EvaluationDeadlineEndedEvent)
        CEP->>EventBus: publishEvent(event)

        Note over EventBus: BEFORE_COMMIT 리스너 실행<br/>(같은 트랜잭션)

        EventBus->>CEL: handleEvaluationDeadlineEnded(event)
        Note over CEL: @Order(1) BEFORE_COMMIT
        CEL->>CMES: deleteAllByMeetupId(meetupId)
        CMES->>DB: DELETE ChatMessage

        EventBus->>PL: handleEvaluationDeadlineEndedEvent(event)
        Note over PL: @Order(2) BEFORE_COMMIT
        PL->>PES: deleteAllByMeetupId(meetupId)
        PES->>DB: DELETE Participant

        Note over MSS: 트랜잭션 커밋
    end
```

**이벤트 데이터**:

```java
class EvaluationDeadlineEndedEvent {
    UUID eventId;
    LocalDateTime occurredAt;
    UUID meetupId;             // 평가 기한이 만료된 모임 ID
}
```

**리스너 처리**:

| 리스너                | 처리 내용               | 순서      | 트랜잭션                      | 비동기 |
| --------------------- | ----------------------- | --------- | ----------------------------- | ------ |
| ChattingEventListener | 채팅 메시지 모두 삭제   | @Order(1) | BEFORE_COMMIT (같은 트랜잭션) | ❌     |
| ParticipantListener   | 참가자 데이터 모두 삭제 | @Order(2) | BEFORE_COMMIT (같은 트랜잭션) | ❌     |

**주의사항**:

- `BEFORE_COMMIT`으로 같은 트랜잭션에서 실행됨
- `@Order`로 실행 순서 보장 (채팅 메시지 먼저, 참가자는 나중)
- 하나라도 실패하면 전체 롤백

---

## 3. 이벤트 흐름 전체 매핑

### 3.1 모임 생명주기와 이벤트

```mermaid
stateDiagram-v2
    [*] --> OPEN: 모임 생성

    OPEN --> IN_PROGRESS: 시작
    OPEN --> CANCELED: 취소

    IN_PROGRESS --> ENDED: 종료<br/> MeetupFinishedEvent
    IN_PROGRESS --> CANCELED: 관리자 취소<br/> MeetupCanceledEvent

    ENDED --> EVALUATION_TIMEOUT: 3일 경과<br/> EvaluationDeadlineEndedEvent
    ENDED --> [*]: 평가 기간 중 유지

    EVALUATION_TIMEOUT --> [*]
    CANCELED --> [*]

    note right of ENDED
        평가 기간 중 (3일)
        참가자들이 서로 평가
        각 평가마다 EvaluationSubmittedEvent 발행
        평가 완료되어도 상태는 그대로 유지
    end note
```

### 3.2 이벤트별 리스너 매핑

```mermaid
graph LR
    subgraph MeetupEvents[모임 이벤트]
        MFE[MeetupFinishedEvent]
        MCE[MeetupCanceledEvent]
    end

    subgraph EvaluationEvents[평가 이벤트]
        ESE[EvaluationSubmittedEvent]
        EDE[EvaluationDeadlineEndedEvent]
    end

    subgraph BadgeListeners[배지 관련]
        BAGL1[BadgeAutoGrantListener<br/>모임 완주 배지]
        BAGL2[BadgeAutoGrantListener<br/>평가 관련 배지]
    end

    subgraph ChatListeners[채팅 관련]
        CEL1[ChattingEventListener<br/>종료 메시지]
        CEL2[ChattingEventListener<br/>취소 메시지]
        CEL3[ChattingEventListener<br/>메시지 삭제]
    end

    subgraph DataCleanup[데이터 정리]
        PL[ParticipantListener<br/>참가자 삭제]
    end

    MFE -->|"@Async"| BAGL1
    MFE -->|"@Async"| CEL1

    MCE -->|"@Async"| CEL2

    ESE -->|"@Async"| BAGL2

    EDE -->|"@Order(1)"| CEL3
    EDE -->|"@Order(2)"| PL

    style MFE fill:#e1f5ff
    style MCE fill:#ffe1e1
    style ESE fill:#e1ffe1
    style EDE fill:#fff3e1
```

---

## 4. 트랜잭션

| 이벤트                       | 리스너                 | 트랜잭션 단계 | 이유                                           |
| ---------------------------- | ---------------------- | ------------- | ---------------------------------------------- |
| MeetupFinishedEvent          | BadgeAutoGrantListener | AFTER_COMMIT  | 배지 부여 실패해도 모임 종료는 완료되어야 함   |
| MeetupFinishedEvent          | ChattingEventListener  | AFTER_COMMIT  | 메시지 발송 실패해도 모임 종료는 완료되어야 함 |
| MeetupCanceledEvent          | ChattingEventListener  | AFTER_COMMIT  | 메시지 발송 실패해도 취소는 완료되어야 함      |
| EvaluationSubmittedEvent     | BadgeAutoGrantListener | AFTER_COMMIT  | 배지 부여 실패해도 평가는 저장되어야 함        |
| EvaluationDeadlineEndedEvent | ChattingEventListener  | BEFORE_COMMIT | 데이터 삭제는 원자적으로 처리되어야 함         |
| EvaluationDeadlineEndedEvent | ParticipantListener    | BEFORE_COMMIT | 데이터 삭제는 원자적으로 처리되어야 함         |
