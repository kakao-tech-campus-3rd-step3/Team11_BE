DROP TABLE IF EXISTS refresh_token CASCADE;
DROP TABLE IF EXISTS member_role CASCADE;
DROP TABLE IF EXISTS role CASCADE;
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
)
