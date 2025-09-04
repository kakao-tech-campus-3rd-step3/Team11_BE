-- password: testpass1212!
INSERT INTO member (id, email, password)
VALUES (
   'e33f271b-83e3-42bd-b909-ac43a8dd0c89',
    'admin@test.com',
    '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G'
);

INSERT INTO member_role (id, member_id, name)
VALUES (
   '02c6f7f8-28ce-4a04-8994-c319e44def8e',
    (SELECT id FROM member WHERE email = 'admin@test.com'),
    'ROLE_ADMIN'
);

-- password: testpass1212!
INSERT INTO member (id, email, password)
VALUES (
        '62344955-c8d3-4032-9352-f3b90f2b4eaa',
           'user@test.com',
           '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G'
       );

INSERT INTO member_role (id, member_id, name)
VALUES (
        '7b75d464-b471-4b80-8fb6-89ae06d9eb3a',
           (SELECT id FROM member WHERE email = 'user@test.com'),
           'ROLE_USER'
       );

INSERT INTO profile (
    id, member_id, nickname, age, gender, image_url, description, base_location
) VALUES (
             'b1a2b3c4-d5e6-7890-abcd-ef1234567890',
             '62344955-c8d3-4032-9352-f3b90f2b4eaa',
             '테스트유저',
             25,
             'MALE',
             'https://cdn.example.com/profiles/user.png',
             '테스트 환경용 기본 프로필',
             '부산 금정구'
         );
