-- password: testpass1212!
INSERT INTO member (email, password, verified)
VALUES (
           'admin@test.com',
           '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G',
              TRUE
       );

INSERT INTO member_role (member_id, name)
VALUES (
           (SELECT id FROM member WHERE email = 'admin@test.com'),
           'ROLE_ADMIN'
       );

-- password: testpass1212!
INSERT INTO member (email, password, verified)
VALUES (
           'user@test.com',
           '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G',
                TRUE
       );

INSERT INTO member_role (member_id, name)
VALUES (
           (SELECT id FROM member WHERE email = 'user@test.com'),
           'ROLE_USER'
       );

-- 'alice' 회원, 역할, 프로필 추가
INSERT INTO member (email, password, verified)
VALUES (
        'alice@test.com',
        '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G',
        TRUE
       );

INSERT INTO member_role (member_id, name)
VALUES (
        (SELECT id FROM member WHERE email = 'alice@test.com'),
        'ROLE_USER'
       );

INSERT INTO profile (member_id, nickname, age, gender, image_url, description, base_location_id)
VALUES (
        (SELECT id FROM member WHERE email = 'alice@test.com'),
        '앨리스',
        24,
        'FEMALE',
        'https://momeet-dev-bucket-1.s3.ap-northeast-2.amazonaws.com/profiles/alice.png',
        '보드게임/카페 모임 좋아해요 ☕',
        26410
       );

-- 'chris' 회원, 역할, 프로필 추가
INSERT INTO member (email, password, verified)
VALUES (
        'chris@test.com',
        '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G',
        TRUE
       );

INSERT INTO member_role (member_id, name)
VALUES (
        (SELECT id FROM member WHERE email = 'chris@test.com'),
        'ROLE_USER'
       );

INSERT INTO profile (member_id, nickname, age, gender, image_url, description, base_location_id)
VALUES (
        (SELECT id FROM member WHERE email = 'chris@test.com'),
        '크리스',
        27,
        'MALE',
        'https://momeet-dev-bucket-1.s3.ap-northeast-2.amazonaws.com/profiles/chris.png',
        '풋살·등산 러버 🏔️',
        26260
       );

INSERT INTO badge (
    id, name, description, icon_url, code, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    '모임 새싹',
    '모임 첫 참여 배지',
    'https://momeet-dev-bucket-1.s3.ap-northeast-2.amazonaws.com/badges/meetup-first.png',
    'FIRST_JOIN',
    now(),
    now()
);

INSERT INTO badge (
    id, name, description, icon_url, code, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    '모임 고수',
    '모임 10회 참여 배지',
    'https://momeet-dev-bucket-1.s3.ap-northeast-2.amazonaws.com/badges/meetup-ten.png',
    'TEN_JOINS',
    now(),
    now()
);

INSERT INTO badge (
    id, name, description, icon_url, code, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    '호감 인기인',
    '좋아요 10개',
    'https://momeet-dev-bucket-1.s3.ap-northeast-2.amazonaws.com/badges/like-five.png',
    'LIKE_10',
    now(),
    now()
);

INSERT INTO badge (
    id, name, description, icon_url, code, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    '[TEST] 테스트용 배지 1',
    '테스트용 배지 1',
    'https://momeet-dev-bucket-1.s3.ap-northeast-2.amazonaws.com/badges/test-1.png',
    'TEST_1',
    now(),
    now()
);

INSERT INTO badge (
    id, name, description, icon_url, code, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    '[TEST] 테스트용 배지 2',
    '테스트용 배지 2',
    'https://momeet-dev-bucket-1.s3.ap-northeast-2.amazonaws.com/badges/test-2.png',
    'TEST_2',
    now(),
    now()
);

INSERT INTO badge (
    id, name, description, icon_url, code, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    '[TEST] 테스트용 배지 3',
    '테스트용 배지 3',
    'https://momeet-dev-bucket-1.s3.ap-northeast-2.amazonaws.com/badges/test-3.png',
    'TEST_3',
    now(),
    now()
);

INSERT INTO profile_badge (id, profile_id, badge_id, created_at, is_representative)
SELECT
    gen_random_uuid(),
    p.id,
    (SELECT id FROM badge WHERE name = '[TEST] 테스트용 배지 1'),
    now(),
    TRUE
FROM profile p
         JOIN member m ON p.member_id = m.id
WHERE m.email = 'alice@test.com';

INSERT INTO profile_badge (id, profile_id, badge_id, created_at, is_representative)
SELECT
    gen_random_uuid(),
    p.id,
    (SELECT id FROM badge WHERE name = '[TEST] 테스트용 배지 2'),
    now(),
    FALSE
FROM profile p
         JOIN member m ON p.member_id = m.id
WHERE m.email = 'alice@test.com';

INSERT INTO profile_badge (id, profile_id, badge_id, created_at, is_representative)
SELECT
    gen_random_uuid(),
    p.id,
    (SELECT id FROM badge WHERE name = '[TEST] 테스트용 배지 3'),
    now(),
    FALSE
FROM profile p
         JOIN member m ON p.member_id = m.id
WHERE m.email = 'alice@test.com';