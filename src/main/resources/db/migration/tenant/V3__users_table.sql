CREATE TABLE IF NOT EXISTS users (
    id                      BIGSERIAL PRIMARY KEY,

    -- Employee Identity
    emp_id                   VARCHAR(50) NOT NULL UNIQUE,
    first_name               VARCHAR(100) NOT NULL,
    last_name                VARCHAR(100) NOT NULL,

    -- Authentication
    email                    VARCHAR(255) UNIQUE,
    username                 VARCHAR(255) NOT NULL UNIQUE,
    password_hash            VARCHAR(512) NOT NULL,

    -- Contact
    contact_number           VARCHAR(15) NOT NULL,
    alternate_number         VARCHAR(15),

    -- Organization
    department               VARCHAR(150) NOT NULL,
    designation              VARCHAR(150) NOT NULL,
    role_id                  BIGINT NOT NULL,

    -- Branch / Hierarchy
    reporting_manager_id     BIGINT,

    -- Employment
    employment_type          VARCHAR(50) NOT NULL,
    date_of_joining          DATE NOT NULL,

    -- Status
    status                   VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_active                BOOLEAN NOT NULL DEFAULT TRUE,

    -- Current Address
    current_address_line1    VARCHAR(255) NOT NULL,
    current_address_line2    VARCHAR(255),
    current_city             VARCHAR(100) NOT NULL,
    current_state            VARCHAR(100) NOT NULL,
    current_country          VARCHAR(100) NOT NULL,
    current_pincode          VARCHAR(20) NOT NULL,

    -- Permanent Address
    permanent_address_line1  VARCHAR(255) NOT NULL,
    permanent_address_line2  VARCHAR(255),
    permanent_city           VARCHAR(100) NOT NULL,
    permanent_state          VARCHAR(100) NOT NULL,
    permanent_country        VARCHAR(100) NOT NULL,
    permanent_pincode        VARCHAR(20) NOT NULL,

    -- Audit
    last_login_at            TIMESTAMPTZ,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Foreign Keys
    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_users_reporting_manager
        FOREIGN KEY (reporting_manager_id)
        REFERENCES users(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_users_emp_id
ON users(emp_id);

CREATE INDEX IF NOT EXISTS idx_users_email
ON users(email);

CREATE INDEX IF NOT EXISTS idx_users_username
ON users(username);

CREATE INDEX IF NOT EXISTS idx_users_role_id
ON users(role_id);

CREATE INDEX IF NOT EXISTS idx_users_reporting_manager_id
ON users(reporting_manager_id);