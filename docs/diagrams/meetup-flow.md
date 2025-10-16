# 모임 생성/조회/삭제 플로우

## 1. 모임 생성 플로우차트

### 전체 프로세스

```mermaid
flowchart TD
    Start([사용자: 모임 생성 요청]) --> Request[POST /api/meetups<br/>MeetupCreateRequest]
    Request --> Auth{인증 확인<br/>@PreAuthorize}

    Auth -->|인증 실패| AuthFail[401 Unauthorized]
    Auth -->|인증 성공| Controller[MeetupController.createMeetup]

    Controller --> ValidateCategory[카테고리 검증<br/>mainCategory와 subCategory 일치?]
    ValidateCategory -->|불일치| CategoryError[400 CustomValidationException<br/>서브 카테고리 오류]

    ValidateCategory -->|일치| CheckExisting{기존 활성 모임 존재?<br/>OPEN or IN_PROGRESS}
    CheckExisting -->|존재함| ExistingError[400 CustomValidationException<br/>이미 진행중인 모임 있음]

    CheckExisting -->|없음| GetProfile[Profile 조회<br/>profileService.getByMemberId]
    GetProfile --> CheckScore{프로필 온도<br/>>= scoreLimit?}

    CheckScore -->|부족| ScoreError[400 CustomValidationException<br/>온도가 제한 점수보다 낮음]

    CheckScore -->|충족| CreatePoint[위치 Point 생성<br/>GeometryFactory.createPoint]
    CreatePoint --> GetSigungu[시군구 조회<br/>sigunguService.getByPointIn]

    GetSigungu --> CreateEntity[Meetup 엔티티 생성<br/>MeetupDtoMapper.toEntity]
    CreateEntity --> SaveMeetup[Meetup 저장<br/>entityService.createMeetup]

    SaveMeetup --> SetHashTags[해시태그 설정<br/>meetup.setHashTags]
    SetHashTags --> AddOwner[소유자를 HOST로<br/>Participant 추가]

    AddOwner --> FetchDetail[상세 조회<br/>fetch join으로 재조회]
    FetchDetail --> Success[201 Created<br/>MeetupDetail 반환]

    AuthFail --> End([종료])
    CategoryError --> End
    ExistingError --> End
    ScoreError --> End
    Success --> End
```

### 상세 검증 로직

```mermaid
flowchart TD
    subgraph Validation[검증 단계]
        V1[1. 카테고리 검증]
        V2[2. 활성 모임 중복 체크]
        V3[3. 프로필 온도 체크]
        V4[4. 지역 정보 조회]

        V1 --> V2 --> V3 --> V4
    end

    subgraph Creation[생성 단계]
        C1[1. Meetup 엔티티 생성]
        C2[2. 해시태그 설정]
        C3[3. 소유자 Participant 추가]
        C4[4. 트랜잭션 커밋]

        C1 --> C2 --> C3 --> C4
    end

    Validation --> Creation
```

---

## 2. 모임 생성 시퀀스 다이어그램

```mermaid
sequenceDiagram
    actor User as 사용자
    participant MC as MeetupController
    participant MDS as MeetupDomainService
    participant MES as MeetupEntityService
    participant PES as ProfileEntityService
    participant SES as SigunguEntityService
    participant GF as GeometryFactory
    participant DB as Database

    User->>MC: POST /api/meetups<br/>(MeetupCreateRequest)
    MC->>MC: @PreAuthorize 인증 체크

    MC->>MDS: createMeetup(request, memberId)

    MDS->>MDS: validateCategories()<br/>(mainCategory, subCategory)
    alt 카테고리 불일치
        MDS-->>MC: CustomValidationException
        MC-->>User: 400 Bad Request
    end

    MDS->>PES: mapToProfileId(memberId)
    PES-->>MDS: profileId

    MDS->>MES: existsByOwnerIdAndStatusIn<br/>(profileId, [OPEN, IN_PROGRESS])
    MES->>DB: SELECT COUNT(*)
    DB-->>MES: count

    alt 이미 활성 모임 존재
        MES-->>MDS: true
        MDS-->>MC: CustomValidationException
        MC-->>User: 400 Bad Request
    end

    MDS->>GF: createPoint(longitude, latitude)
    GF-->>MDS: Point

    MDS->>PES: getByMemberId(memberId)
    PES->>DB: SELECT Profile
    DB-->>PES: Profile
    PES-->>MDS: profile

    MDS->>MDS: temperature >= scoreLimit?
    alt 온도 부족
        MDS-->>MC: CustomValidationException
        MC-->>User: 400 Bad Request
    end

    MDS->>SES: getByPointIn(locationPoint)
    SES->>DB: SELECT Sigungu<br/>(PostGIS ST_Contains)
    DB-->>SES: Sigungu
    SES-->>MDS: sigungu

    MDS->>MDS: MeetupDtoMapper.toEntity()
    MDS->>MES: createMeetup(meetup, hashTags)
    MES->>DB: INSERT Meetup
    DB-->>MES: savedMeetup
    MES->>DB: INSERT MeetupHashTags
    MES-->>MDS: createdMeetup

    MDS->>MDS: meetup.addParticipant<br/>(HOST role)
    MDS->>DB: INSERT Participant

    MDS->>MES: getByIdWithDetails(meetupId)
    MES->>DB: SELECT Meetup<br/>(fetch join)
    DB-->>MES: meetupDetail
    MES-->>MDS: meetupDetail

    MDS-->>MC: MeetupDetail
    MC-->>User: 201 Created<br/>Location: /api/meetups/{id}
```

---

## 3. 모임 조회 플로우

### 페이지네이션 조회

```mermaid
flowchart TD
    Start([사용자: 모임 목록 조회]) --> Request[GET /api/meetups?page=0&size=10]
    Request --> Auth{인증 확인}
    Auth -->|실패| AuthFail[401 Unauthorized]
    Auth -->|성공| Controller[MeetupController.meetupPage]

    Controller --> ParseRequest[MeetupPageRequest 파싱<br/>category, status, search 등]
    ParseRequest --> ToSpec[Specification 변환<br/>동적 쿼리 조건 생성]
    ToSpec --> ToPageable[PageRequest 생성<br/>정렬 조건 포함]

    ToPageable --> Query[MeetupEntityService<br/>getAllBySpecification]
    Query --> DB[(Database<br/>JPA Specification)]
    DB --> MapResponse[MeetupResponse 매핑<br/>Page로 변환]
    MapResponse --> Success[200 OK<br/>Page MeetupResponse]

    AuthFail --> End([종료])
    Success --> End
```

### 지역 기반 조회 (PostGIS)

```mermaid
flowchart TD
    Start([사용자: 주변 모임 검색]) --> Request[GET /api/meetups/geo<br/>?lat=35.1&lon=129.0&radius=5000]
    Request --> Auth{인증 확인}
    Auth -->|실패| AuthFail[401 Unauthorized]
    Auth -->|성공| Controller[MeetupController.meetupGeoSearch]

    Controller --> ParseRequest[MeetupGeoSearchRequest 파싱]
    ParseRequest --> CreatePoint[Point 생성<br/>GeometryFactory]
    CreatePoint --> Query[getAllByLocation<br/>위경도 + 반경 + 필터]

    Query --> PostGIS[(PostGIS<br/>ST_DWithin 쿼리)]
    PostGIS --> Filter[추가 필터링<br/>category, subCategory, search]
    Filter --> MapResponse[List MeetupResponse 매핑]
    MapResponse --> Success[200 OK<br/>List MeetupResponse]

    AuthFail --> End([종료])
    Success --> End
```

### 상세 조회

```mermaid
sequenceDiagram
    actor User as 사용자
    participant MC as MeetupController
    participant MDS as MeetupDomainService
    participant MES as MeetupEntityService
    participant DB as Database

    User->>MC: GET /api/meetups/{meetupId}
    MC->>MC: @PreAuthorize 인증 체크

    MC->>MDS: getById(meetupId)
    MDS->>MES: getByIdWithDetails(meetupId)
    MES->>DB: SELECT Meetup<br/>LEFT JOIN FETCH owner<br/>LEFT JOIN FETCH participants<br/>LEFT JOIN FETCH hashTags

    alt 모임 없음
        DB-->>MES: empty
        MES-->>MDS: NoSuchElementException
        MDS-->>MC: NoSuchElementException
        MC-->>User: 404 Not Found
    end

    DB-->>MES: Meetup (with details)
    MES-->>MDS: meetup
    MDS->>MDS: MeetupEntityMapper.toDetail()
    MDS-->>MC: MeetupDetail
    MC-->>User: 200 OK MeetupDetail
```

---

## 4. 모임 상태 전이 다이어그램

```mermaid
stateDiagram-v2
    [*] --> OPEN: 모임 생성

    OPEN --> IN_PROGRESS: 시작<br/>(POST /me/start)
    OPEN --> CANCELED: 취소<br/>(POST /me/cancel)<br/>또는 관리자 취소

    IN_PROGRESS --> ENDED: 종료<br/>(POST /me/finish)
    IN_PROGRESS --> CANCELED: 관리자 취소<br/>(POST /{id}/cancel)

    ENDED --> EVALUATION_TIMEOUT: 평가 기한 만료<br/>(스케줄러, 24시간 후)
    ENDED --> [*]: 평가 기간 중 유지

    EVALUATION_TIMEOUT --> [*]: 완전 종료
    CANCELED --> [*]: 완전 종료

    note right of OPEN
        모집 중
        참가자 입장 가능
        수정 가능
    end note

    note right of IN_PROGRESS
        진행 중
        새 참가자 불가
        수정 불가
    end note

    note right of ENDED
        종료됨
        평가 기간 (24시간)
        평가 완료되어도 상태 유지
    end note

    note right of EVALUATION_TIMEOUT
        평가 기한 초과
        데이터 정리 후 종료
    end note
```

---

## 5. 모임 상태 변경 시퀀스

### 모임 시작

```mermaid
sequenceDiagram
    actor User as 모임 소유자
    participant MSC as MeetupStateController
    participant MSS as MeetupStateService
    participant MES as MeetupEntityService
    participant PES as ProfileEntityService
    participant DB as Database

    User->>MSC: POST /api/meetups/me/start
    MSC->>MSC: @PreAuthorize 인증

    MSC->>MSS: startMeetupByMemberId(memberId)
    MSS->>PES: mapToProfileId(memberId)
    PES-->>MSS: ownerProfileId

    MSS->>MES: getAllByOwnerIdAndStatusIn<br/>(profileId, [OPEN])
    MES->>DB: SELECT Meetup

    alt 시작 가능한 모임 없음
        DB-->>MES: empty
        MES-->>MSS: []
        MSS-->>MSC: NoSuchElementException
        MSC-->>User: 404 Not Found
    end

    DB-->>MES: [meetup]
    MES-->>MSS: meetup

    MSS->>MES: updateMeetup(meetup, status=IN_PROGRESS)
    MES->>DB: UPDATE Meetup SET status='IN_PROGRESS'
    DB-->>MES: OK

    MSS-->>MSC: 완료
    MSC-->>User: 204 No Content
```

### 모임 종료 (이벤트 발행)

```mermaid
sequenceDiagram
    actor User as 모임 소유자
    participant MSC as MeetupStateController
    participant MSS as MeetupStateService
    participant MES as MeetupEntityService
    participant PES as ParticipantEntityService
    participant CEP as CoreEventPublisher
    participant DB as Database
    participant EventBus as Spring Event Bus

    User->>MSC: POST /api/meetups/me/finish
    MSC->>MSS: finishMeetupByMemberId(memberId)

    MSS->>MES: getAllByOwnerIdAndStatusIn<br/>(profileId, [IN_PROGRESS])
    MES->>DB: SELECT Meetup

    alt 종료 가능한 모임 없음
        MES-->>MSS: []
        MSS-->>MSC: IllegalStateException
        MSC-->>User: 400 Bad Request
    end

    MES-->>MSS: meetup

    MSS->>MSS: finishMeetupInternal(meetup)

    Note over MSS: 트랜잭션 시작

    MSS->>MES: updateMeetup<br/>(status=ENDED, endAt=now)
    MES->>DB: UPDATE Meetup

    MSS->>PES: getAllByMeetupId(meetupId)
    PES->>DB: SELECT Participants
    DB-->>PES: participants
    PES-->>MSS: participants

    MSS->>MSS: 각 참가자 Profile에<br/>completedJoinMeetups++
    MSS->>DB: UPDATE Profile

    MSS->>CEP: publish(MeetupFinishedEvent)
    CEP->>CEP: 로깅
    CEP->>EventBus: publishEvent(event)

    Note over MSS: 트랜잭션 커밋

    EventBus->>EventBus: @TransactionalEventListener<br/>AFTER_COMMIT 리스너들 실행

    MSS-->>MSC: 완료
    MSC-->>User: 204 No Content
```

### 모임 취소

```mermaid
sequenceDiagram
    actor User as 모임 소유자/관리자
    participant MSC as MeetupStateController
    participant MSS as MeetupStateService
    participant MES as MeetupEntityService
    participant CEP as CoreEventPublisher
    participant DB as Database

    alt 사용자 취소
        User->>MSC: POST /api/meetups/me/cancel
        MSC->>MSS: cancelMeetup(memberId)
        MSS->>MES: getAllByOwnerIdAndStatusIn<br/>(profileId, [OPEN])

        alt 취소 가능한 모임 없음
            MES-->>MSS: []
            MSS-->>MSC: NoSuchElementException
            MSC-->>User: 404 Not Found
        end

        MES-->>MSS: meetup
        MSS->>MES: updateMeetup(status=CANCELED)
        Note over MSS: 이벤트 발행 없음 (사용자 취소)
    else 관리자 취소
        User->>MSC: POST /api/meetups/{id}/cancel
        MSC->>MSC: @PreAuthorize('ROLE_ADMIN')
        MSC->>MSS: cancelMeetupAdmin(meetupId)
        MSS->>MES: getById(meetupId)

        alt OPEN/IN_PROGRESS 아님
            MSS-->>MSC: IllegalStateException
            MSC-->>User: 400 Bad Request
        end

        MSS->>MES: updateMeetup(status=CANCELED)
        MSS->>CEP: publish(MeetupCanceledEvent<br/>requestedBy=ROLE_ADMIN)
        CEP->>DB: Event 발행
    end

    MES->>DB: UPDATE Meetup
    MSS-->>MSC: 완료
    MSC-->>User: 204 No Content
```

---

## 핵심 컴포넌트 설명

### MeetupController

- 모임 CRUD HTTP 요청 처리
- 페이지네이션, 지역 검색, 상세 조회 엔드포인트

### MeetupStateController

- 모임 상태 변경 전용 컨트롤러
- 시작, 종료, 취소 엔드포인트

### MeetupDomainService

- 모임 생성/조회/수정 비즈니스 로직
- 복잡한 검증 로직 처리
- DTO ↔ Entity 변환

### MeetupStateService

- 모임 상태 전이 비즈니스 로직
- 이벤트 발행 책임
- 트랜잭션 관리

### MeetupEntityService

- Meetup 엔티티 CRUD
- 데이터베이스 직접 접근
- 단순 쿼리 처리

### GeometryFactory

- JTS (Java Topology Suite)
- 위경도를 Point 객체로 변환
- PostGIS와 호환

### PostGIS

- PostgreSQL 공간 확장
- `ST_Contains`: 점이 영역 내에 있는지
- `ST_DWithin`: 두 지점 간 거리 계산

### CoreEventPublisher

- Spring Event 발행 래퍼
- 로깅 추가
- DomainEvent 전파
