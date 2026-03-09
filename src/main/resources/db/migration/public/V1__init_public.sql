-- ============================================================
-- V1__init_public.sql  –  PUBLIC (global/shared) schema objects
-- Runs automatically at application startup via PublicFlywayConfig.
-- All tables here live in the 'public' schema.
-- ============================================================

-- ── Tenant Registry ──────────────────────────────────────────
-- Stores every onboarded tenant.  schema_name = PostgreSQL schema name.
CREATE TABLE IF NOT EXISTS public.tenant_registry (
    id            BIGSERIAL    PRIMARY KEY,
    tenant_name   VARCHAR(100) NOT NULL,
    schema_name   VARCHAR(100) NOT NULL UNIQUE,
    display_name  VARCHAR(255),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | SUSPENDED | DELETED
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_tenant_registry_schema_name
    ON public.tenant_registry (schema_name);

-- ── Platform Roles ────────────────────────────────────────────
-- Global platform roles shared across all tenants (e.g. SUPER_ADMIN, PLATFORM_SUPPORT).
CREATE TABLE IF NOT EXISTS public.roles (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ── Platform Modules ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.modules (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- ── Platform Actions ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.actions (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,   -- READ, WRITE, DELETE, EXECUTE …
    description VARCHAR(255)
);

-- ── Root / MetaAdmin User ─────────────────────────────────────
-- The single super-admin user that manages the platform itself.
CREATE TABLE IF NOT EXISTS public.root_user (
    id           BIGSERIAL    PRIMARY KEY,
    username     VARCHAR(150) NOT NULL UNIQUE,
    email        VARCHAR(255) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    role         VARCHAR(255) NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ── Seed Data ─────────────────────────────────────────────────
INSERT INTO public.roles (name, description)
VALUES
    ('CEO', 'Full organizational control'),
    ('BRANCH_MANAGER', 'Manages branch operations'),
    ('BRANCH_IN_CHARGE', 'Handles branch daily execution'),
    ('TECHNICIAN_MANAGER', 'Manages technician teams'),
    ('TECHNICIAN', 'Executes technical tasks'),
    ('ACCOUNT_MANAGER', 'Supervises accounting operations'),
    ('ACCOUNT_PERSON', 'Handles accounting entries and vouchers'),
    ('SALES_MANAGER', 'Supervises sales team'),
    ('SALES_PERSON', 'Handles sales execution and lead conversion'),
    ('HR', 'Manages human resources'),
    ('OPERATION_HEAD', 'Controls operational workflow')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.modules (name, description)
VALUES
    ('BRANCH_MANAGEMENT', 'Manage branches and branch configuration'),
    ('USER_MANAGEMENT', 'Manage users and user access'),
    ('ROLE_MANAGEMENT', 'Manage roles and permissions'),
    ('PRODUCT_MANAGEMENT', 'Manage products and inventory items'),
    ('TAX_MANAGEMENT', 'Manage tax rules and tax configuration')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.actions (name, description)
VALUES
    ('READ', 'Read resource'),
    ('ADD', 'Create new resource'),
    ('EDIT', 'Modify existing resource'),
    ('DELETE', 'Delete resource'),
    ('APPROVE', 'Approve business operation'),
    ('REQUEST', 'Submit request for approval'),
    ('EXPORT', 'Export data'),
    ('DOWNLOAD', 'Download files or reports')
ON CONFLICT (name) DO NOTHING;
