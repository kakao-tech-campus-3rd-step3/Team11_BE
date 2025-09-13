CREATE SCHEMA IF NOT EXISTS public_test;

-- 외래키 의존성 순서대로 DROP
DROP TABLE IF EXISTS public_test.meetup_participant CASCADE;
DROP TABLE IF EXISTS public_test.meetup_hash_tag CASCADE;
DROP TABLE IF EXISTS public_test.meetup CASCADE;
DROP TABLE IF EXISTS public_test.refresh_token CASCADE;
DROP TABLE IF EXISTS public_test.member_role CASCADE;
DROP TABLE IF EXISTS public_test.role CASCADE;
DROP TABLE IF EXISTS public_test.profile CASCADE;
DROP TABLE IF EXISTS public_test.member CASCADE;

CREATE TABLE public_test.member (
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


CREATE TABLE public_test.member_role (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id uuid NOT NULL REFERENCES public_test.member(id) ON DELETE CASCADE,
    name varchar(50) NOT NULL,
granted_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE public_test.refresh_token (
    member_id uuid PRIMARY KEY REFERENCES public_test.member(id) ON DELETE CASCADE,
    token_value varchar(255) NOT NULL
);

CREATE TABLE public_test.profile (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id                UUID      NOT NULL UNIQUE REFERENCES public_test.member(id) ON DELETE CASCADE,
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
CREATE UNIQUE INDEX IF NOT EXISTS uq_profile_nickname_ci ON public_test.profile (LOWER(btrim(nickname)));


CREATE TABLE public_test.meetup (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID        NOT NULL REFERENCES public_test.profile(id),
    name            VARCHAR(60) NOT NULL,
    category        VARCHAR(30) NOT NULL,
    sub_category    VARCHAR(30) NOT NULL,
    description     TEXT        NOT NULL,
    participant_count INTEGER     NOT NULL DEFAULT 1,
    capacity        INTEGER     NOT NULL DEFAULT 10,
    score_limit     DOUBLE PRECISION NOT NULL DEFAULT 36.5,
    location_point  geography(Point, 4326) NOT NULL,
    address         TEXT        NOT NULL,
    sgg_code        BIGINT      NOT NULL REFERENCES public.sigungu_boundary(sgg_code),
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    end_at          TIMESTAMP,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_meetup_location_point ON public_test.meetup USING GIST (location_point);
CREATE INDEX IF NOT EXISTS idx_meetup_status ON public_test.meetup (status);
CREATE INDEX IF NOT EXISTS idx_meetup_owner ON public_test.meetup (owner_id);
CREATE INDEX IF NOT EXISTS idx_category ON public_test.meetup (category);
CREATE INDEX IF NOT EXISTS idx_sub_category ON public_test.meetup (sub_category);

CREATE TABLE public_test.meetup_hash_tag (
    id              BIGSERIAL PRIMARY KEY,
    meetup_id       UUID NOT NULL REFERENCES public_test.meetup(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE public_test.meetup_participant (
    id              BIGSERIAL PRIMARY KEY,
    meetup_id       UUID NOT NULL REFERENCES public_test.meetup(id) ON DELETE CASCADE,
    profile_id      UUID NOT NULL REFERENCES public_test.profile(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    is_rated        BOOLEAN NOT NULL DEFAULT FALSE,
    last_active_at  TIMESTAMP,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE(meetup_id, profile_id)
);

CREATE INDEX IF NOT EXISTS idx_meetup_participant_meetup ON public_test.meetup_participant (meetup_id);
CREATE INDEX IF NOT EXISTS idx_meetup_participant_profile ON public_test.meetup_participant (profile_id);
