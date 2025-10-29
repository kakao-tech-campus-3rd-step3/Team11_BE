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
        'https://cdn.example.com/profiles/alice.png',
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
        'https://cdn.example.com/profiles/bob.png',
        '풋살·등산 러버 🏔️',
        26260
       );

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
    'https://momeet-dev-bucket-1.s3.ap-northeast-2.amazonaws.com/profiles/meetupOwner.png',
    '방장1 테스트 프로필',
    26260
);

-- 테스트 모임 1: 광안 농구 모임
INSERT INTO meetup (
    owner_id, name, category, description, participant_count, capacity, score_limit, location_point,
    address, sgg_code, status, start_at, end_at
)
VALUES (
           (SELECT id FROM profile WHERE nickname = '방장1'),
           '광안 농구 모임',
           'SPORTS',
           '광안리 근처 농구장 같이 뛰실 분 구합니다! 🏀',
           1,
           10,
           36.0,
           ST_GeomFromText('POINT(129.08225 35.23103)', 4326),
           '부산광역시 수영구 광안동 농구장',
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
    'https://momeet-dev-bucket-1.s3.ap-northeast-2.amazonaws.com/profiles/meetupOwner2.png',
    '방장2 테스트 프로필',
    26260
);

-- ⚽ 테스트 모임 2: 광안 풋살 번개
INSERT INTO meetup (
    owner_id, name, category, description, participant_count, capacity, score_limit,
    location_point, address, sgg_code, status, start_at, end_at
)
VALUES (
           (SELECT id FROM profile WHERE nickname = '방장2'),
           '광안 풋살 번개',
           'SPORTS',
           '초보 환영 ⚽ 광안리 풋살장 5대5 경기 예정입니다!',
           1,
           12,
           35.0,
           ST_GeomFromText('POINT(129.0785 35.2287)', 4326),
           '부산광역시 수영구 민락동 풋살장',
           26410,
           'OPEN',
           '2025-11-05 19:30:00',
           '2025-11-05 22:30:00'
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
           'https://momeet-dev-bucket-1.s3.ap-northeast-2.amazonaws.com/profiles/meetupOwner3.png',
           '방장3 테스트 프로필',
           26260
       );

-- 🏐 테스트 모임 3: 광안 해변 배구 모임
INSERT INTO meetup (
    owner_id, name, category, description, participant_count, capacity, score_limit,
    location_point, address, sgg_code, status, start_at, end_at
)
VALUES (
           (SELECT id FROM profile WHERE nickname = '방장3'),
           '광안 해변 배구 모임',
           'SPORTS',
           '광안리 해변에서 즐기는 배구 모임! ☀️',
           1,
           8,
           36.5,
           ST_GeomFromText('POINT(129.1173 35.1534)', 4326),
           '부산광역시 수영구 광안해변로',
           26410,
           'OPEN',
           '2025-11-05 19:30:00',
           '2025-11-05 22:30:00'
       );

-- 🏷 해시태그 추가
INSERT INTO meetup_hash_tag (meetup_id, name, created_at)
SELECT m.id, t.tag, now()
FROM (
         VALUES
             ('광안 농구 모임','방장1','#농구'),
             ('광안 농구 모임','방장1','#운동'),
             ('광안 풋살 번개','방장2','#풋살'),
             ('광안 풋살 번개','방장2','#축구'),
             ('광안 해변 배구 모임','방장3','#배구'),
             ('광안 해변 배구 모임','방장3','#바다')
     ) AS t(meetup_name, owner_nickname, tag)
         JOIN meetup  m ON m.name = t.meetup_name
         JOIN profile p ON p.id = m.owner_id AND p.nickname = t.owner_nickname
;

-- 방장 참가자 추가
INSERT INTO meetup_participant (meetup_id, profile_id, role, is_active)
SELECT m.id, p.id, 'HOST', TRUE
FROM (
         VALUES
             ('광안 농구 모임','방장1'),
             ('광안 풋살 번개','방장2'),
             ('광안 해변 배구 모임','방장3')
     ) AS t(meetup_name, owner_nickname)
         JOIN meetup  m ON m.name = t.meetup_name
         JOIN profile p ON p.nickname = t.owner_nickname AND p.id = m.owner_id
;
