DO $$
    DECLARE
        admin_id uuid;
    BEGIN
        -- password: testpass1212!
        INSERT INTO member (email, password)
        VALUES ('admin@test.com', '$2a$10$c.KAjYSgNz6KLUtG7Qw0B.i/vviGv/FgKvMH7orJFvx8Oh0.wmJ5G')
        RETURNING id INTO admin_id;

        INSERT INTO member_role (member_id, name)
        VALUES (admin_id, 'ROLE_ADMIN');
    END $$;