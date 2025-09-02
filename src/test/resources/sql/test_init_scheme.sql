DROP TABLE IF EXISTS refresh_token CASCADE;
DROP TABLE IF EXISTS member_role CASCADE;
DROP TABLE IF EXISTS role CASCADE;
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
