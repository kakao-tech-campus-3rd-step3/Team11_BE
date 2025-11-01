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
        'https://www.momeet.click/profiles/alice.png',
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
        'https://www.momeet.click/profiles/chris.png',
        '풋살·등산 러버 🏔️',
        26260
       );

INSERT INTO badge (
    id, name, description, icon_url, code, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    '모임 새싹',
    '모임 첫 참여 배지',
    'https://www.momeet.click/badges/meetup-first.png',
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
    'https://www.momeet.click/badges/meetup-ten.png',
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
    'https://www.momeet.click/badges/like-five.png',
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
    'https://www.momeet.click/badges/test-1.png',
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
    'https://www.momeet.click/badges/test-2.png',
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
    'https://www.momeet.click/badges/test-3.png',
    'TEST_3',
    now(),
    now()
);

INSERT INTO profile_badge (id, profile_id, badge_id, created_at, updated_at, is_representative)
SELECT
    gen_random_uuid(),
    p.id,
    (SELECT id FROM badge WHERE name = '[TEST] 테스트용 배지 1'),
    now(),
    now(),
    TRUE
FROM profile p
         JOIN member m ON p.member_id = m.id
WHERE m.email = 'alice@test.com';

INSERT INTO profile_badge (id, profile_id, badge_id, created_at, updated_at, is_representative)
SELECT
    gen_random_uuid(),
    p.id,
    (SELECT id FROM badge WHERE name = '[TEST] 테스트용 배지 2'),
    now(),
    now(),
    FALSE
FROM profile p
         JOIN member m ON p.member_id = m.id
WHERE m.email = 'alice@test.com';

INSERT INTO profile_badge (id, profile_id, badge_id, created_at, updated_at, is_representative)
SELECT
    gen_random_uuid(),
    p.id,
    (SELECT id FROM badge WHERE name = '[TEST] 테스트용 배지 3'),
    now(),
    now(),
    FALSE
FROM profile p
         JOIN member m ON p.member_id = m.id
WHERE m.email = 'alice@test.com';

-- 테스트 모임 생성용 계정 1
-- password: testpass1212!
INSERT INTO member (email, password, verified)
VALUES (
           'meetupowner1@test.com',
           '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G',
           TRUE
       );

INSERT INTO member_role (member_id, name)
VALUES (
           (SELECT id FROM member WHERE email = 'meetupowner1@test.com'),
           'ROLE_USER'
       );

INSERT INTO profile (member_id, nickname, age, gender, image_url, description, base_location_id)
VALUES (
    (SELECT id FROM member WHERE email = 'meetupowner1@test.com'),
    '방장1',
    30,
    'MALE',
    'https://www.momeet.click/profiles/meetupOwner.png',
    '방장1 테스트 프로필',
    26260
);

-- 테스트 모임 1: 금정 농구 모임 (장전동, PNU 근처)
INSERT INTO meetup (
    owner_id, name, category, description, participant_count, capacity, score_limit, location_point,
    address, sgg_code, status, start_at, end_at
)
VALUES (
           (SELECT id FROM profile WHERE nickname = '방장1'),
           '금정 농구 모임',
           'SPORTS',
           '장전동 체육공원 근처 농구 한 판! 🏀',
           1,
           10,
           36.0,
           ST_GeomFromText('POINT(129.0890 35.2350)', 4326),
           '부산광역시 금정구 장전동',
           26410,
           'OPEN',
           '2025-11-05 19:30:00',
           '2025-11-05 22:30:00'
       );

-- 테스트 모임 생성용 계정 2
-- password: testpass1212!
INSERT INTO member (email, password, verified)
VALUES (
    'meetupowner2@test.com',
    '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G',
    TRUE
);

INSERT INTO member_role (member_id, name)
VALUES (
    (SELECT id FROM member WHERE email = 'meetupowner2@test.com'),
    'ROLE_USER'
);

INSERT INTO profile (member_id, nickname, age, gender, image_url, description, base_location_id)
VALUES (
    (SELECT id FROM member WHERE email = 'meetupowner2@test.com'),
    '방장2',
    50,
    'MALE',
    'https://www.momeet.click/profiles/meetupOwner2.png',
    '방장2 테스트 프로필',
    26260
);

-- 테스트 모임 2: 금정 풋살 번개 (구서동)
INSERT INTO meetup (
    owner_id, name, category, description, participant_count, capacity, score_limit,
    location_point, address, sgg_code, status, start_at, end_at
)
VALUES (
           (SELECT id FROM profile WHERE nickname = '방장2'),
           '금정 풋살 번개',
           'SPORTS',
           '구서동 풋살장 5대5 번개 ⚽ 초보 환영!',
           1,
           12,
           35.0,
           ST_GeomFromText('POINT(129.0920 35.2460)', 4326),
           '부산광역시 금정구 구서동',
           26410,
           'OPEN',
           '2025-11-06 19:30:00',
           '2025-11-06 22:30:00'
       );

-- 테스트 모임 생성용 계정 3
-- password: testpass1212!
INSERT INTO member (email, password, verified)
VALUES (
           'meetupowner3@test.com',
           '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G',
           TRUE
       );

INSERT INTO member_role (member_id, name)
VALUES (
           (SELECT id FROM member WHERE email = 'meetupowner3@test.com'),
           'ROLE_USER'
       );

INSERT INTO profile (member_id, nickname, age, gender, image_url, description, base_location_id)
VALUES (
           (SELECT id FROM member WHERE email = 'meetupowner3@test.com'),
           '방장3',
           33,
           'FEMALE',
           'https://www.momeet.click/profiles/meetupOwner3.png',
           '방장3 테스트 프로필',
           26260
       );

-- 테스트 모임 3: 금정 배구 모임 (남산동)
INSERT INTO meetup (
    owner_id, name, category, description, participant_count, capacity, score_limit,
    location_point, address, sgg_code, status, start_at, end_at
)
VALUES (
           (SELECT id FROM profile WHERE nickname = '방장3'),
           '금정 배구 모임',
           'SPORTS',
           '남산동 실내 체육관에서 배구 같이 해요! 🏐',
           1,
           8,
           36.5,
           ST_GeomFromText('POINT(129.0860 35.2590)', 4326),
           '부산광역시 금정구 남산동',
           26410,
           'OPEN',
           '2025-11-07 19:30:00',
           '2025-11-07 22:30:00'
       );

-- 방장 HOST 참가자 추가
INSERT INTO meetup_participant (meetup_id, profile_id, role, is_active)
SELECT m.id, p.id, 'HOST', TRUE
FROM (
         VALUES
             ('금정 농구 모임','방장1'),
             ('금정 풋살 번개','방장2'),
             ('금정 배구 모임','방장3')
     ) t(meetup_name, owner_nickname)
         JOIN meetup  m ON m.name = t.meetup_name
         JOIN profile p ON p.id = m.owner_id AND p.nickname = t.owner_nickname;

-- 해시태그 추가
INSERT INTO meetup_hash_tag (meetup_id, name, created_at)
SELECT m.id, tag, now()
FROM (
         VALUES
             ('금정 농구 모임','방장1','농구'),
             ('금정 농구 모임','방장1','운동'),
             ('금정 풋살 번개','방장2','풋살'),
             ('금정 풋살 번개','방장2','축구'),
             ('금정 배구 모임','방장3','배구'),
             ('금정 배구 모임','방장3','실내')
     ) t(mname, nick, tag)
         JOIN meetup  m ON m.name = t.mname
         JOIN profile p ON p.id = m.owner_id AND p.nickname = t.nick;