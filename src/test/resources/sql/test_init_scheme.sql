DROP TABLE IF EXISTS refresh_token CASCADE;
DROP TABLE IF EXISTS member_role CASCADE;
DROP TABLE IF EXISTS role CASCADE;
DROP TABLE IF EXISTS profile CASCADE;
DROP TABLE IF EXISTS member CASCADE;

CREATE TABLE member (
    id             VARCHAR(36) PRIMARY KEY,
    email          VARCHAR(255) UNIQUE NOT NULL,
    password       VARCHAR(255) DEFAULT NULL,
    provider       VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    provider_id    VARCHAR(255) DEFAULT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    token_issued_at  TIMESTAMP DEFAULT NULL,
    enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    is_account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(provider, provider_id)
);

CREATE TABLE member_role (
    id VARCHAR(36) PRIMARY KEY,
    member_id VARCHAR(36) NOT NULL,
    name VARCHAR(50) NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    UNIQUE(member_id, name),
    CONSTRAINT fk_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE
);

CREATE TABLE refresh_token (
    member_id VARCHAR(36) PRIMARY KEY,
    token_value VARCHAR(255) NOT NULL,
    CONSTRAINT fk_member_refresh FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE
);

CREATE TABLE profile (
    id                       VARCHAR(36) PRIMARY KEY,
    member_id                VARCHAR(36) NOT NULL UNIQUE,
    nickname                 VARCHAR(20) NOT NULL,
    nickname_ci              VARCHAR(20) GENERATED ALWAYS AS (LOWER(TRIM(nickname))) UNIQUE,
    age                      INTEGER NOT NULL,
    gender                   VARCHAR(10) NOT NULL,
    image_url                VARCHAR(255),
    description              VARCHAR(500),
    base_location            VARCHAR(100) NOT NULL,
    temperature              DECIMAL(4,1) NOT NULL DEFAULT 36.5,
    likes                    INTEGER NOT NULL DEFAULT 0,
    dislikes                 INTEGER NOT NULL DEFAULT 0,
    un_evaluated_meetup_id   BIGINT,
    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    updated_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    CONSTRAINT fk_profile_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    CONSTRAINT ck_profile_nickname_len
        CHECK (CHAR_LENGTH(TRIM(nickname)) BETWEEN 2 AND 20),
    CONSTRAINT ck_profile_age_range
        CHECK (age IS NULL OR (age BETWEEN 14 AND 100)),
    CONSTRAINT ck_profile_gender
        CHECK (gender IN ('MALE','FEMALE')),
    CONSTRAINT ck_profile_temperature_range
        CHECK (temperature BETWEEN 0.0 AND 100.0),
    CONSTRAINT ck_profile_likes_nonneg
        CHECK (likes >= 0),
    CONSTRAINT ck_profile_dislikes_nonneg
        CHECK (dislikes >= 0)
);

