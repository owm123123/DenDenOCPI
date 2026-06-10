ALTER TABLE users
    ADD COLUMN public_id UUID DEFAULT gen_random_uuid() NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uk_users_public_id UNIQUE (public_id);
