-- Schema: public

-- 1. Create the table
CREATE TABLE IF NOT EXISTS public.global_users (
    id              BIGSERIAL PRIMARY KEY,

    -- Authentication
    email           VARCHAR(255) NOT NULL UNIQUE,
    username        VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(512) NOT NULL, -- BCrypt/Argon2

    -- Core Profile
    full_name       VARCHAR(100),
    phone_number    VARCHAR(50),

    -- Tenant Routing
    -- This tells the app which schema to load for this user.
    -- If a user can belong to multiple tenants, move this to a 'memberships' table.
    target_schema   VARCHAR(63),
    has_schema      BOOLEAN NOT NULL DEFAULT FALSE,

    -- System Role (To distinguish Your Staff vs. Customers)
    system_role     VARCHAR(50) DEFAULT 'CEO', -- e.g., 'SUPER_ADMIN', 'CUSTOMER'

    -- System Status
    is_active       BOOLEAN DEFAULT TRUE,

    -- Audit
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

-- 2. Performance Indexes
CREATE INDEX IF NOT EXISTS idx_global_users_email
ON public.global_users(email);

CREATE INDEX IF NOT EXISTS idx_global_users_username
ON public.global_users(username);