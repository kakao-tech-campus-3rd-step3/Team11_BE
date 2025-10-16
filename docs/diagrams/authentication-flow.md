# 로그인/회원가입 플로우

## 1. 이메일 회원가입 및 로그인

### 회원가입 시퀀스

```mermaid
sequenceDiagram
    actor User as 사용자
    participant AC as AuthController
    participant EAS as EmailAuthService
    participant MES as MemberEntityService
    participant JWT as JwtTokenProvider
    participant TCM as TokenCookieManager
    participant DB as Database
    participant Redis as Redis

    User->>AC: POST /api/auth/signup<br/>(email, password1, password2)
    AC->>AC: 비밀번호 일치 검증
    AC->>EAS: signUp(email, password)

    EAS->>MES: createMember(Member)
    MES->>DB: Member 엔티티 저장
    DB-->>MES: savedMember
    MES-->>EAS: savedMember

    EAS->>MES: updateMember()<br/>(tokenIssuedAt, enabled=true)
    EAS->>JWT: generateAccessToken(memberId)
    JWT-->>EAS: accessToken
    EAS->>JWT: generateRefreshToken(memberId)
    JWT-->>EAS: refreshToken

    EAS->>Redis: save RefreshToken
    Redis-->>EAS: OK

    EAS-->>AC: TokenResponse
    AC->>TCM: saveAccessTokenToCookie()
    AC->>TCM: saveRefreshTokenToCookie()
    AC-->>User: 200 OK + Set-Cookie
```

### 로그인 시퀀스

```mermaid
sequenceDiagram
    actor User as 사용자
    participant AC as AuthController
    participant EAS as EmailAuthService
    participant MES as MemberEntityService
    participant JWT as JwtTokenProvider
    participant TCM as TokenCookieManager
    participant DB as Database
    participant Redis as Redis

    User->>AC: POST /api/auth/login<br/>(email, password)
    AC->>EAS: login(email, password)

    EAS->>MES: getByEmail(email)
    MES->>DB: SELECT Member
    DB-->>MES: Member

    alt 존재하지 않는 이메일
        MES-->>EAS: NoSuchElementException
        EAS-->>AC: AuthenticationException
        AC-->>User: 401 Unauthorized
    end

    EAS->>EAS: Provider 검증 (EMAIL인지)
    alt Provider가 EMAIL이 아님
        EAS-->>AC: AuthenticationException
        AC-->>User: 401 Unauthorized
    end

    EAS->>EAS: passwordEncoder.matches()
    alt 비밀번호 불일치
        EAS-->>AC: AuthenticationException
        AC-->>User: 401 Unauthorized
    end

    EAS->>MES: updateMember()<br/>(tokenIssuedAt, enabled=true)
    EAS->>JWT: generateAccessToken(memberId)
    JWT-->>EAS: accessToken
    EAS->>JWT: generateRefreshToken(memberId)
    JWT-->>EAS: refreshToken

    EAS->>Redis: save RefreshToken
    Redis-->>EAS: OK

    EAS-->>AC: TokenResponse
    AC->>TCM: saveAccessTokenToCookie()
    AC->>TCM: saveRefreshTokenToCookie()
    AC-->>User: 200 OK + Set-Cookie
```

---

## 2. 카카오 OAuth 로그인

### 카카오 로그인 전체 플로우

```mermaid
sequenceDiagram
    actor User as 사용자
    participant Browser as 브라우저
    participant KAC as KakaoAuthController
    participant KAS as KakaoAuthService
    participant Kakao as 카카오 API
    participant MES as MemberEntityService
    participant JWT as JwtTokenProvider
    participant TCM as TokenCookieManager
    participant DB as Database
    participant Redis as Redis

    User->>Browser: 카카오 로그인 클릭
    Browser->>KAC: GET /api/auth/kakao
    KAC->>KAS: getKakaoAuthUrl()
    KAS-->>KAC: 카카오 인증 URL
    KAC-->>Browser: redirect to 카카오 인증 페이지

    Browser->>Kakao: 카카오 로그인 페이지
    User->>Kakao: 카카오 계정 입력 및 동의
    Kakao-->>Browser: redirect with code

    Browser->>KAC: GET /api/auth/kakao/callback?code=xxx
    KAC->>KAS: kakaoLogin(code)

    KAS->>KAS: getAccessTokenFromKakao(code)
    KAS->>Kakao: POST /oauth/token<br/>(code, client_id, client_secret)
    Kakao-->>KAS: accessToken

    KAS->>KAS: getKakaoUserInfoFromToken(token)
    KAS->>Kakao: GET /v2/user/me<br/>(Bearer token)
    Kakao-->>KAS: KakaoUserInfo(kakaoId, email)

    KAS->>MES: getByEmail(email)

    alt 기존 회원 존재
        MES->>DB: SELECT Member
        DB-->>MES: Member

        KAS->>KAS: Provider 검증 (KAKAO인지)
        alt Provider가 KAKAO가 아님
            KAS-->>KAC: AuthenticationException
            KAC-->>Browser: 401 Unauthorized
        end

        KAS->>KAS: 계정 잠김 검증
        alt 계정이 잠김
            KAS-->>KAC: BannedAccountException
            KAC-->>Browser: 403 Forbidden
        end
    else 신규 회원
        MES-->>KAS: NoSuchElementException
        KAS->>MES: saveMember(Member)<br/>(email, KAKAO, kakaoId)
        MES->>DB: INSERT Member
        DB-->>MES: savedMember
    end

    KAS->>MES: updateMember()<br/>(tokenIssuedAt, enabled=true)
    KAS->>JWT: generateAccessToken(memberId)
    JWT-->>KAS: accessToken
    KAS->>JWT: generateRefreshToken(memberId)
    JWT-->>KAS: refreshToken

    KAS->>Redis: save RefreshToken
    Redis-->>KAS: OK

    KAS-->>KAC: TokenResponse
    KAC->>TCM: saveAccessTokenToCookie()
    KAC->>TCM: saveRefreshTokenToCookie()
    KAC-->>Browser: 200 OK + Set-Cookie
    Browser-->>User: 로그인 완료
```

---

## 3. 토큰 갱신 플로우

```mermaid
sequenceDiagram
    actor User as 사용자
    participant AC as AuthController
    participant EAS as EmailAuthService
    participant JWT as JwtTokenProvider
    participant Redis as Redis
    participant MES as MemberEntityService
    participant DB as Database
    participant TCM as TokenCookieManager

    User->>AC: POST /api/auth/refresh<br/>(refreshToken in Cookie)
    AC->>EAS: refreshTokens(refreshToken)

    EAS->>JWT: getPayload(refreshToken)
    JWT->>JWT: 토큰 파싱 및 검증

    alt 토큰 만료
        JWT-->>EAS: ExpiredJwtException
        EAS-->>AC: AuthenticationException
        AC-->>User: 401 Unauthorized
    end

    alt 토큰 파싱 실패
        JWT-->>EAS: Exception
        EAS-->>AC: IllegalArgumentException
        AC-->>User: 400 Bad Request
    end

    JWT-->>EAS: Claims (memberId)

    EAS->>Redis: findById(memberId)

    alt RefreshToken 없음 (로그아웃 상태)
        Redis-->>EAS: empty
        EAS-->>AC: AuthenticationException
        AC-->>User: 401 Unauthorized
    end

    Redis-->>EAS: savedToken

    EAS->>EAS: 토큰 일치 검증
    alt 토큰 불일치
        EAS-->>AC: IllegalArgumentException
        AC-->>User: 400 Bad Request
    end

    EAS->>MES: getById(memberId)
    MES->>DB: SELECT Member
    DB-->>MES: Member

    EAS->>MES: updateMember()<br/>(tokenIssuedAt, enabled=true)
    EAS->>JWT: generateAccessToken(memberId)
    JWT-->>EAS: new accessToken
    EAS->>JWT: generateRefreshToken(memberId)
    JWT-->>EAS: new refreshToken

    EAS->>Redis: save new RefreshToken
    Redis-->>EAS: OK

    EAS-->>AC: TokenResponse
    AC->>TCM: saveAccessTokenToCookie()
    AC->>TCM: saveRefreshTokenToCookie()
    AC-->>User: 200 OK + Set-Cookie
```

---

## 핵심 컴포넌트 설명

### AuthController

- 인증 관련 HTTP 요청 처리
- 회원가입, 로그인, 토큰 갱신 엔드포인트 제공

### EmailAuthService

- 이메일 기반 인증 비즈니스 로직
- 비밀번호 검증, 토큰 발급 처리

### KakaoAuthService

- 카카오 OAuth 2.0 인증 처리
- 카카오 API와 통신하여 사용자 정보 조회
- 기존 회원 확인 또는 신규 회원 생성

### JwtTokenProvider

- JWT 토큰 생성 및 검증
- Access Token (짧은 유효기간)
- Refresh Token (긴 유효기간)

### TokenCookieManager

- 토큰을 HTTP 쿠키에 저장
- HttpOnly, Secure 속성 설정

### MemberEntityService

- Member 엔티티 CRUD 처리
- 회원 조회, 생성, 수정

### Redis

- Refresh Token 저장소
- 로그아웃 시 삭제하여 토큰 무효화
