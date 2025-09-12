-- password: testpass1212!
INSERT INTO member (email, password)
VALUES (
           'admin@test.com',
           '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G'
       );

INSERT INTO member_role (member_id, name)
VALUES (
           (SELECT id FROM member WHERE email = 'admin@test.com'),
           'ROLE_ADMIN'
       );

-- password: testpass1212!
INSERT INTO member (email, password)
VALUES (
           'user@test.com',
           '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G'
       );

INSERT INTO member_role (member_id, name)
VALUES (
           (SELECT id FROM member WHERE email = 'user@test.com'),
           'ROLE_USER'
       );

-- 'alice' 회원, 역할, 프로필 추가
INSERT INTO member (email, password)
VALUES (
        'alice@test.com',
        '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G'
       );

INSERT INTO member_role (member_id, name)
VALUES (
        (SELECT id FROM member WHERE email = 'alice@test.com'),
        'ROLE_USER'
       );

INSERT INTO profile (member_id, nickname, age, gender, image_url, description, base_location)
VALUES (
        (SELECT id FROM member WHERE email = 'alice@test.com'),
        '앨리스',
        24,
        'FEMALE',
        'https://cdn.example.com/profiles/alice.png',
        '보드게임/카페 모임 좋아해요 ☕',
        '부산 금정구'
       );

-- 'chris' 회원, 역할, 프로필 추가
INSERT INTO member (email, password)
VALUES (
        'chris@test.com',
        '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G'
       );

INSERT INTO member_role (member_id, name)
VALUES (
        (SELECT id FROM member WHERE email = 'chris@test.com'),
        'ROLE_USER'
       );

INSERT INTO profile (member_id, nickname, age, gender, image_url, description, base_location)
VALUES (
        (SELECT id FROM member WHERE email = 'chris@test.com'),
        '크리스',
        27,
        'MALE',
        'https://cdn.example.com/profiles/bob.png',
        '풋살·등산 러버 🏔️',
        '부산 남구'
       );

INSERT INTO meetup(owner_id, name, category, sub_category,description,capacity,score_limit,location_point,address,sgg_code,status,end_at)
VALUES (
        (SELECT id FROM profile WHERE member_id = (SELECT id FROM member WHERE email = 'alice@test.com')),
        '부산대 근처 보드게임 카페에서 보드게임 같이 해요!',
        'GAME',
        'BOARD_GAME',
        '부산대 근처 보드게임 카페에서 매주 일요일 오후 2시에 보드게임 같이 할 사람 구해요! 초보자도 환영합니다.',
        8,
        36,
        ST_SetSRID(ST_MakePoint(129.08262659183725, 35.23203443995263), 4326)::geography,
        '부산광역시 금정구 부산대학로 63번길 2',
        26410,
        'OPEN',
        now() + interval '8 hours'
);

INSERT INTO meetup(owner_id, name, category, sub_category,description,capacity,score_limit,location_point,address,sgg_code,status,end_at)
VALUES (
        (SELECT id FROM profile WHERE member_id = (SELECT id FROM member WHERE email = 'chris@test.com')),
        '부산대 농구장에서 같이 농구해요!',
        'SPORTS',
        'BASKETBALL',
        '매주 토요일 오후 3시에 부산대 농구장에서 같이 농구할 사람 구해요! 초보자도 환영합니다.',
        10,
        36,
        ST_SetSRID(ST_MakePoint(129.08262659183725, 35.23203443995263), 4326)::geography,
        '부산광역시 금정구 부산대학로 63번길 2',
        26410,
        'OPEN',
        now() + interval '10 hours'
);


