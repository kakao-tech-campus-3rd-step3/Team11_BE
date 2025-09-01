DO $$
    DECLARE
        admin_id uuid;
        user1_id UUID;
        user2_id UUID;
    BEGIN
        -- password: testpass1212!
        INSERT INTO member (email, password)
        VALUES ('admin@test.com', '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G')
        RETURNING id INTO admin_id;

        INSERT INTO member (email, password)
        VALUES ('alice@test.com', '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G')
            RETURNING id INTO user1_id;

        INSERT INTO member (email, password)
        VALUES ('bob@test.com', '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G')
            RETURNING id INTO user2_id;

        INSERT INTO member_role (member_id, name)
        VALUES (admin_id, 'ROLE_ADMIN');

        INSERT INTO member_role (member_id, name)
        VALUES (user1_id, 'ROLE_USER');

        INSERT INTO member_role (member_id, name)
        VALUES (user2_id, 'ROLE_USER');

        INSERT INTO profile (
            member_id, nickname, age, gender, image_url, description, base_location
        ) VALUES
              (user1_id, '앨리스', 24, 'FEMALE',
               'https://cdn.example.com/profiles/alice.png',
               '보드게임/카페 모임 좋아해요 ☕', '부산 금정구'),
              (user2_id, '밥', 27, 'MALE',
               'https://cdn.example.com/profiles/bob.png',
               '풋살·등산 러버 🏔️', '부산 남구');
    END $$;