DROP TABLE IF EXISTS refresh_token CASCADE;
DROP TABLE IF EXISTS member_role CASCADE;
DROP TABLE IF EXISTS role CASCADE;
DROP TABLE IF EXISTS badge_condition_mapping CASCADE;
DROP TABLE IF EXISTS badge_condition CASCADE;
DROP TABLE IF EXISTS profile_badge CASCADE;
DROP TABLE IF EXISTS badge CASCADE;
DROP TABLE IF EXISTS profile CASCADE;
DROP TABLE IF EXISTS member CASCADE;

CREATE TABLE member (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email          varchar(255) UNIQUE NOT NULL,
    password       varchar(255) DEFAULT NULL,                       -- 외부 인증만 사용할 땐 NULL 가능
    provider       VARCHAR(20) NOT NULL DEFAULT 'EMAIL',-- EMAIL/KAKAO/GOOGLE/...
    provider_id    varchar(255) DEFAULT NULL,                        -- 외부 식별자(소셜)
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    token_issued_at  TIMESTAMP DEFAULT NULL,
    enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    is_account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(provider, provider_id)
);


CREATE TABLE member_role (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id uuid NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    name varchar(50) NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_token (
    member_id uuid PRIMARY KEY REFERENCES member(id) ON DELETE CASCADE,
    token_value varchar(255) NOT NULL
);

CREATE TABLE profile (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id                UUID      NOT NULL UNIQUE REFERENCES member(id) ON DELETE CASCADE,
    nickname                 VARCHAR(20) NOT NULL,
    age                      INTEGER NOT NULL,
    gender                   VARCHAR(10) NOT NULL,
    image_url                VARCHAR(255),
    description              VARCHAR(500),
    base_location            VARCHAR(100) NOT NULL,
    temperature              NUMERIC(4,1) NOT NULL DEFAULT 36.5,
    likes                    INTEGER      NOT NULL DEFAULT 0,
    dislikes                 INTEGER      NOT NULL DEFAULT 0,
    un_evaluated_meetup_id   BIGINT,
    created_at               TIMESTAMP  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_profile_nickname_len        CHECK (char_length(btrim(nickname)) BETWEEN 2 AND 20),
    CONSTRAINT ck_profile_age_range           CHECK (age IS NULL OR (age BETWEEN 14 AND 100)),
    CONSTRAINT ck_profile_gender              CHECK (gender IN ('MALE','FEMALE')),
    CONSTRAINT ck_profile_temperature_range   CHECK (temperature BETWEEN 0.0 AND 100.0),
    CONSTRAINT ck_profile_likes_nonneg        CHECK (likes    >= 0),
    CONSTRAINT ck_profile_dislikes_nonneg     CHECK (dislikes >= 0)
);

-- 닉네임 대소문자 무시 유니크
CREATE UNIQUE INDEX IF NOT EXISTS uq_profile_nickname_ci ON profile (LOWER(btrim(nickname)));

CREATE TABLE badge_condition (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     code VARCHAR(50) UNIQUE NOT NULL, -- 조건 키 값 (예: FIRST_JOIN, TEN_JOINS)
     description VARCHAR(255), -- 조건 설명 (운영/개발 참고용)
     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
     updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE badge (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(20) UNIQUE NOT NULL,
    description VARCHAR(255),
    icon_url VARCHAR(255) NOT NULL, -- 배지 아이콘 URL
    condition_id UUID NOT NULL REFERENCES badge_condition(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE profile_badge (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES profile(id) ON DELETE CASCADE,
    badge_id UUID NOT NULL REFERENCES badge(id) ON DELETE CASCADE,
    granted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_representative BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (profile_id, badge_id)
);

CREATE TABLE badge_condition_mapping (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    badge_id UUID NOT NULL REFERENCES badge(id) ON DELETE CASCADE,
    condition_id UUID NOT NULL REFERENCES badge_condition(id) ON DELETE CASCADE,
    operator VARCHAR(10) NOT NULL DEFAULT 'AND' -- 조건 결합 방식 (AND/OR)
);