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
