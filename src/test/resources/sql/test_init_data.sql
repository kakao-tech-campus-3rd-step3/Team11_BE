-- password: testpass1212!
INSERT INTO public_test.member (email, password)
VALUES (
    'admin@test.com',
    '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G'
);

INSERT INTO public_test.member_role (member_id, name)
VALUES (
    (SELECT id FROM public_test.member WHERE email = 'admin@test.com'),
    'ROLE_ADMIN'
);

-- Admin용 프로필 생성
INSERT INTO public_test.profile (
    member_id, nickname, age, gender, image_url, description, base_location
) VALUES (
    (SELECT id FROM public_test.member WHERE email = 'admin@test.com'),
    '관리자',
    30,
    'MALE',
    'https://cdn.example.com/profiles/admin.png',
    '테스트 환경용 관리자 프로필',
    '부산 해운대구'
);

-- password: testpass1212!
INSERT INTO public_test.member (email, password)
VALUES (
           'user@test.com',
           '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G'
       );

INSERT INTO public_test.member_role (member_id, name)
VALUES (
           (SELECT id FROM public_test.member WHERE email = 'user@test.com'),
           'ROLE_USER'
       );

INSERT INTO public_test.profile (
    member_id, nickname, age, gender, image_url, description, base_location
) VALUES (
             (SELECT id FROM public_test.member WHERE email = 'user@test.com'),
             '테스트유저',
             25,
             'MALE',
             'https://cdn.example.com/profiles/user.png',
             '테스트 환경용 기본 프로필',
             '부산 금정구'
         );

-- 종료된 모임 추가 (관리자가 owner)
INSERT INTO public_test.meetup (
    id, owner_id, name, category, sub_category, description,
    participant_count, capacity, score_limit, location_point, address, sgg_code,
    status, end_at, created_at, updated_at
) VALUES (
gen_random_uuid(),
    (SELECT id FROM public_test.profile WHERE nickname = '관리자'),
    '종료된 테스트 모임',
    'SPORTS',
    'SOCCER',
    '테스트 환경용 종료된 모임입니다.',
    2, -- owner + user 참가자
    10,
    36.5,
    ST_GeomFromText('POINT(129.059 35.153)', 4326),
    '부산 서면 ○○카페',
    26410,
    'ENDED',
    now() - interval '1 day',
    now(),
    now()
);

-- 유저 참가자로 등록
INSERT INTO public_test.meetup_participant (
    meetup_id, profile_id, role, is_active, is_rated, created_at
) VALUES (
    (SELECT id FROM public_test.meetup WHERE name = '종료된 테스트 모임'),
    (SELECT id FROM public_test.profile WHERE nickname = '테스트유저'),
    'MEMBER',
    true,
    false,
    now()
);

-- owner도 participant로 들어가야 한다면 추가
INSERT INTO public_test.meetup_participant (
    meetup_id, profile_id, role, is_active, is_rated, created_at
) VALUES (
    (SELECT id FROM public_test.meetup WHERE name = '종료된 테스트 모임'),
    (SELECT id FROM public_test.profile WHERE nickname = '관리자'),
    'HOST',
    true,
    false,
    now()
);

INSERT INTO public_test.badge (
    id, name, description, icon_url, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    '[TEST] 모임 새싹',
    '테스트용: 첫 참여 배지',
    'https://static.example.com/badges/test-first.png',
    now(),
    now()
);

INSERT INTO public_test.badge (
    id, name, description, icon_url, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    '[TEST] 모임 고수',
    '테스트용: 10회 참여 배지',
    'https://static.example.com/badges/test-10.png',
    now(),
    now()
);

INSERT INTO public_test.badge (
    id, name, description, icon_url, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    '[TEST] 호감 인기인',
    '테스트용: 좋아요 5개',
    'https://static.example.com/badges/test-like5.png',
    now(),
    now()
);

INSERT INTO public_test.profile_badge (id, profile_id, badge_id, created_at, is_representative)
SELECT
    gen_random_uuid(),
    p.id,
    (SELECT id FROM public_test.badge WHERE name = '[TEST] 모임 고수'),
    now(),
    TRUE
FROM public_test.profile p
         JOIN public_test.member m ON p.member_id = m.id
WHERE m.email = 'admin@test.com';

INSERT INTO public_test.profile_badge (id, profile_id, badge_id, created_at, is_representative)
SELECT
    gen_random_uuid(),
    p.id,
    (SELECT id FROM public_test.badge WHERE name = '[TEST] 모임 새싹'),
    now(),
    FALSE
FROM public_test.profile p
         JOIN public_test.member m ON p.member_id = m.id
WHERE m.email = 'admin@test.com';

INSERT INTO public_test.profile_badge (id, profile_id, badge_id, created_at, is_representative)
SELECT
    gen_random_uuid(),
    p.id,
    (SELECT id FROM public_test.badge WHERE name = '[TEST] 호감 인기인'),
    now(),
    FALSE
FROM public_test.profile p
         JOIN public_test.member m ON p.member_id = m.id
WHERE m.email = 'user@test.com';