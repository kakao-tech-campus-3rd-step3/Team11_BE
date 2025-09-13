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