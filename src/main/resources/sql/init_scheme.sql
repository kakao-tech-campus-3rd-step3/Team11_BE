DROP TABLE IF EXISTS evaluation CASCADE;
DROP TABLE IF EXISTS chat_message CASCADE;
DROP TABLE IF EXISTS refresh_token CASCADE;
DROP TABLE IF EXISTS member_role CASCADE;
DROP TABLE IF EXISTS role CASCADE;
DROP TABLE IF EXISTS profile CASCADE;
DROP TABLE IF EXISTS member CASCADE;
DROP TABLE IF EXISTS meetup CASCADE;
DROP TABLE IF EXISTS meetup_hash_tag CASCADE;
DROP TABLE IF EXISTS meetup_participant CASCADE;
DROP TABLE IF EXISTS badge CASCADE;
DROP TABLE IF EXISTS profile_badge CASCADE;

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

CREATE TABLE IF NOT EXISTS public.sigungu_boundary (
     sgg_code      BIGINT PRIMARY KEY,
     sido_code     BIGINT      NOT NULL,
     sido_name     VARCHAR(255) NOT NULL,
     sgg_name      VARCHAR(255) NOT NULL,
     geom          geometry(Polygon, 4326),
     base_location geometry(Point, 4326),
     created_at    TIMESTAMP,
     updated_at    TIMESTAMP
);

CREATE TABLE meetup (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID        NOT NULL REFERENCES profile(id),
    name            VARCHAR(60) NOT NULL,
    category        VARCHAR(30) NOT NULL,
    sub_category    VARCHAR(30) NOT NULL,
    description     TEXT        NOT NULL,
    participant_count INTEGER     NOT NULL DEFAULT 1,
    capacity        INTEGER     NOT NULL DEFAULT 10,
    score_limit     DOUBLE PRECISION NOT NULL DEFAULT 36.5,
    location_point  geography(Point, 4326) NOT NULL,
    address         TEXT        NOT NULL,
    sgg_code        BIGINT      NOT NULL REFERENCES sigungu_boundary(sgg_code),
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    end_at          TIMESTAMP,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_meetup_location_point ON meetup USING GIST (location_point);
CREATE INDEX IF NOT EXISTS idx_meetup_status ON meetup (status);
CREATE INDEX IF NOT EXISTS idx_meetup_owner ON meetup (owner_id);
CREATE INDEX IF NOT EXISTS idx_category ON meetup (category);
CREATE INDEX IF NOT EXISTS idx_sub_category ON meetup (sub_category);

CREATE TABLE meetup_hash_tag (
    id              BIGSERIAL PRIMARY KEY,
    meetup_id       UUID NOT NULL REFERENCES meetup(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE meetup_participant (
    id              BIGSERIAL PRIMARY KEY,
    meetup_id       UUID NOT NULL REFERENCES meetup(id) ON DELETE CASCADE,
    profile_id      UUID NOT NULL REFERENCES profile(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    is_active       BOOLEAN NOT NULL DEFAULT FALSE,
    is_rated        BOOLEAN NOT NULL DEFAULT FALSE,
    last_active_at  TIMESTAMP,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE(meetup_id, profile_id)
);

CREATE INDEX IF NOT EXISTS idx_meetup_participant_meetup ON meetup_participant (meetup_id);
CREATE INDEX IF NOT EXISTS idx_meetup_participant_profile ON meetup_participant (profile_id);


CREATE TABLE chat_message (
    id              BIGSERIAL PRIMARY KEY,
    meetup_id       UUID NOT NULL REFERENCES meetup(id) ON DELETE CASCADE,
    sender_id       BIGINT REFERENCES meetup_participant(id) ON DELETE SET NULL,
    profile_id      UUID NOT NULL REFERENCES profile(id) ON DELETE SET NULL,
    message_type    VARCHAR(20) NOT NULL,  -- TEXT/IMAGE/SYSTEM
    content         TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_chat_message_meetup ON chat_message (meetup_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_sender ON chat_message (sender_id);

CREATE TABLE evaluation (
    id UUID PRIMARY KEY,
    meetup_id UUID  NOT NULL,
    evaluator_profile_id UUID  NOT NULL,
    target_profile_id UUID  NOT NULL,
    rating SMALLINT NOT NULL, -- 0=LIKE, 1=DISLIKE
    ip_hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_evaluation UNIQUE (meetup_id, evaluator_profile_id, target_profile_id, ip_hash),
    CONSTRAINT fk_evaluation_meetup FOREIGN KEY (meetup_id) REFERENCES meetup(id),
    CONSTRAINT fk_evaluation_evaluator_profile FOREIGN KEY (evaluator_profile_id) REFERENCES profile(id),
    CONSTRAINT fk_evaluation_target_profile FOREIGN KEY (target_profile_id) REFERENCES profile(id)
);


CREATE TABLE IF NOT EXISTS badge (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(20) UNIQUE NOT NULL,
    description VARCHAR(255),
    icon_url VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS profile_badge (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES profile(id) ON DELETE CASCADE,
    badge_id UUID NOT NULL REFERENCES badge(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_representative BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_profile_badge UNIQUE(profile_id, badge_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_one_rep_badge_per_profile
    ON profile_badge(profile_id) WHERE is_representative = TRUE;
CREATE INDEX IF NOT EXISTS idx_profile_badge_profile ON profile_badge(profile_id);
CREATE INDEX IF NOT EXISTS idx_profile_badge_badge   ON profile_badge(badge_id);
CREATE INDEX IF NOT EXISTS idx_profile_rep_only
    ON profile_badge(profile_id) WHERE is_representative = TRUE;