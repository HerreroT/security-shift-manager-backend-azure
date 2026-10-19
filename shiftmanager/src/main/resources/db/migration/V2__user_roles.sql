-- Users get a role. Existing users keep full access as ADMIN.

ALTER TABLE users ADD COLUMN role VARCHAR(20) DEFAULT 'ADMIN' NOT NULL;

ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'SUPERVISOR'));
