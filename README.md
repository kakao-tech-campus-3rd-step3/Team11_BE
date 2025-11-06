# Team11_BE

## 프로젝트 한 줄 소개 ✨

- 즉석/근거리 번개 모임을 빠르게 만들고 참여하는 프로젝트의 백엔드 설계 및 구현

## 개발 환경 🛠️

### Application
- **Framework**: Spring Boot 3 + Spring Data JPA + QueryDSL
- **Security**: Spring Security + JWT
- **Database**: Redis, PostgreSQL + PostGIS
- **Logging**: Logback

### 기술 스택
- **Language**: Java 21
- **Framework**: Spring Boot 3
- **Security**: Spring Security + OAuth2 + JWT
- **Persistence**: Spring Data JPA + QueryDSL
- **Database**: PostgreSQL + PostGIS (공간 데이터 처리)
- **Cache**: Redis (캐싱 및 TTL 관리)
- **Real-time**: WebSocket + STOMP
- **Storage**: AWS S3 (이미지 파일 저장)
- **Email**: Brevo SMTP (이메일 인증)
- **Testing**: JUnit 5 + RestAssured (단위 테스트, E2E 테스트)

## 핵심 기술 구현 🎯

### 1. 이벤트 기반 아키텍처 (Event-Driven Architecture)
- **Domain Event 패턴**: 도메인 이벤트를 통한 느슨한 결합 설계
- **Spring Event Publisher**: `@EventListener`와 `@Async`를 활용한 비동기 이벤트 처리
- **트랜잭션 단계별 처리**:
  - `BEFORE_COMMIT`: 원자적 데이터 정리 (평가 기한 만료 시 채팅/참가자 삭제)
  - `AFTER_COMMIT`: 부수 효과 처리 (배지 부여, 채팅 메시지 발송)
- **이벤트 종류**:
  - `MeetupFinishedEvent`: 모임 종료 시 배지 부여 및 채팅 종료 메시지
  - `MeetupCanceledEvent`: 모임 취소 시 채팅 종료 메시지
  - `EvaluationSubmittedEvent`: 평가 제출 시 배지 부여
  - `EvaluationDeadlineEndedEvent`: 평가 기한 만료 시 데이터 정리
  - `SendVerificationEmailEvent`: 이메일 인증 코드 발송

### 2. Redis 캐싱 및 TTL 관리
- **Spring Cache Abstraction**: `@Cacheable`, `@CacheEvict`를 통한 캐시 관리
- **RedisTemplate**: 커스텀 Redis 템플릿을 통한 인증 코드 저장
- **TTL 관리**:
  - 인증 코드: 설정 가능한 만료 시간 (기본 10분)
  - 캐시 데이터: 기본 10분 TTL 설정
- **Jackson2JsonRedisSerializer**: 객체 직렬화/역직렬화 지원

### 3. QueryDSL을 통한 동적 쿼리
- **타입 안전한 쿼리**: 컴파일 타임에 쿼리 오류 검출
- **동적 쿼리 빌더**: 복잡한 검색 조건을 타입 안전하게 구성
- **Repository 패턴**: `*DslRepository` 인터페이스를 통한 복잡한 쿼리 분리

### 4. PostGIS를 활용한 공간 데이터 처리
- **공간 데이터 타입**: `geography(Point, 4326)`를 통한 좌표 저장
- **공간 쿼리**:
  - `ST_Contains`: 영역 포함 검색 (시군구 매핑)
  - `ST_DWithin`: 거리 기반 검색 (반경 내 모임 조회)
- **GIST 인덱스**: 공간 데이터 조회 성능 최적화
- **시군구 경계면 데이터**: 공공 데이터를 활용한 위치 메타데이터 추가

### 5. WebSocket STOMP를 통한 실시간 통신
- **STOMP 프로토콜**: 메시징 프로토콜을 통한 구조화된 통신
- **JWT 기반 인증**: WebSocket 핸드셰이크 시 JWT 토큰 검증
- **메시지 분기 처리**:
  - `ChatMessageType.TEXT/IMAGE/SYSTEM`: 일반 메시지 처리
  - `ChatActionType`: 사용자 활동 추적 (입장/퇴장/타이핑 등)
- **인터셉터**: `MessageAuthenticateInterceptor`를 통한 메시지 레벨 인증
- **템플릿 패턴**: `ChatMessagingTemplate`을 통한 메시지 발송 추상화

### 6. AWS S3를 통한 파일 관리
- **MultipartFile 처리**: Spring의 `MultipartFile`을 통한 파일 업로드
- **S3 클라이언트**: AWS SDK를 통한 파일 업로드/삭제
- **이미지 검증**: 커스텀 `@ValidImage` 어노테이션을 통한 파일 타입/크기 검증
- **프로필 이미지 관리**: 프로필 삭제 시 S3 이미지 자동 정리 (이벤트 기반)

### 7. 커스텀 Validation 어노테이션
- **`@ValidMainCategory`**: 모임 카테고리 검증
- **`@ValidLocation`**: 좌표 범위 검증
- **`@ValidImage`**: 이미지 파일 타입/크기 검증
- **`@Password`**: 비밀번호 정책 검증
- **`@MeetupTimeUnit`**: 모임 시간 30분 단위 검증
- **`@AllowSortFields`**: 정렬 필드 허용 목록 검증

### 8. 트랜잭션 관리 전략
- **읽기 전용 트랜잭션**: `@Transactional(readOnly = true)`를 통한 조회 성능 최적화
- **트랜잭션 전파**:
  - `REQUIRES_NEW`: 배지 부여 등 독립적인 트랜잭션 필요 시
  - `AFTER_COMMIT`: 부수 효과를 트랜잭션 커밋 후 처리
  - `BEFORE_COMMIT`: 원자적 데이터 정리 필요 시
- **트랜잭션 순서 제어**: `@Order`를 통한 리스너 실행 순서 보장

### 9. 도메인 주도 설계 (DDD) 패턴
- **도메인 모델**: 엔티티와 값 객체를 통한 도메인 표현
- **Repository 패턴**: 데이터 접근 계층 추상화
- **Domain Service**: 도메인 로직 캡슐화
- **Entity Service**: 엔티티 CRUD 작업 분리
- **Event 기반 통신**: 도메인 간 느슨한 결합

### 10. 예외 처리 및 에러 응답
- **Global Exception Handler**: `@ControllerAdvice`를 통한 전역 예외 처리
- **WebSocket Exception Handler**: WebSocket 전용 예외 처리
- **ProblemDetail 표준**: RFC 7807 표준을 따르는 에러 응답 형식
- **커스텀 예외**: 도메인별 예외 클래스 정의
  - `UnVerifiedAccountException`: 미인증 계정
  - `BannedAccountException`: 차단된 계정
  - `MailSendFailureException`: 이메일 발송 실패

### 11. 비동기 처리
- **ThreadPoolTaskExecutor**: 커스텀 스레드 풀 설정
  - Core Pool Size: 4
  - Max Pool Size: 8
  - Queue Capacity: 200
- **@Async**: 이벤트 리스너 및 부수 효과를 비동기로 처리
- **AfterCommitExecutor**: 트랜잭션 커밋 후 실행되는 작업 처리

### 12. 로깅 전략
- **Logback**: Rolling 방식의 로그 파일 관리
- **구조화된 로깅**: `LogTags`를 통한 로그 태그 관리
- **이벤트 로깅**: 도메인 이벤트 발생 시 상세 로깅

### 13. 보안 구현
- **JWT 토큰**: Access Token, Refresh Token, WebSocket Upgrade Token
- **토큰 만료 시간 관리**: 각 토큰 타입별 다른 만료 시간 설정
- **CORS 설정**: 환경별 허용 Origin 설정
- **화이트리스트 URL**: 인증 불필요한 엔드포인트 관리
- **IP 해싱**: 평가 중복 제출 방지를 위한 IP 해싱

### 14. 테스트 전략
- **단위 테스트**: 도메인 로직 및 서비스 레이어 테스트
- **E2E 테스트**: RestAssured를 통한 API 통합 테스트
- **Mock 설정**: `TestMailConfig`를 통한 이메일 발송 Mock 처리
- **테스트 격리**: 각 테스트마다 독립적인 데이터 사용

## 아키텍처 특징 🏗️

### 레이어드 아키텍처
```
Controller Layer (REST API, WebSocket)
    ↓
Service Layer (Domain Service, Entity Service)
    ↓
Repository Layer (JPA Repository, QueryDSL Repository)
    ↓
Database (PostgreSQL + PostGIS, Redis)
```

### 이벤트 기반 통신
- 도메인 이벤트를 통한 서비스 간 느슨한 결합
- 비동기 이벤트 처리를 통한 성능 최적화
- 트랜잭션 단계별 이벤트 처리로 데이터 일관성 보장

### 도메인 모델링
- 엔티티와 값 객체를 통한 도메인 표현
- Repository 패턴을 통한 데이터 접근 추상화
- Domain Service를 통한 도메인 로직 캡슐화

## 진행 상태 🚧

- ✅ 인증/인가 시스템 구현 완료
- ✅ 위치 기반 모임 검색/생성 구현 완료
- ✅ 실시간 채팅 구현 완료
- ✅ 프로필 및 평가 시스템 구현 완료
- ✅ 배지 시스템 구현 완료
- ✅ 이벤트 기반 아키텍처 구현 완료
- ✅ 단위 테스트 및 E2E 테스트 작성 완료
