
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,

    -- Authentication
    email           VARCHAR(255) NOT NULL UNIQUE,
    username        VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(512) NOT NULL,

    -- Profile
    full_name       VARCHAR(100),
    phone_number    VARCHAR(50),

    -- Single Role Assignment
    role_id         BIGINT NOT NULL,

    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Foreign Key
    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_users_email
ON users(email);

CREATE INDEX IF NOT EXISTS idx_users_username
ON users(username);

CREATE INDEX IF NOT EXISTS idx_users_role_id
ON users(role_id);