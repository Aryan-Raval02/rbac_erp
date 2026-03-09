-- ============================================================
-- V1__init_tenant.sql  –  PER-TENANT schema objects
-- Applied programmatically by TenantMigrationService when a
-- new tenant is created.  The target schema is set via
-- Flyway.schemas(tenantName) before execution.
-- ============================================================

-- ── Roles ─────────────────────────────────────────────────────
-- Tenant-scoped roles (e.g. ADMIN, MANAGER, EMPLOYEE).
CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ── Modules ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS modules (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- ── Actions ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS actions (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,   -- READ, WRITE, DELETE, EXECUTE …
    description VARCHAR(255)
);

-- ── Role–Action Permissions ───────────────────────────────────
CREATE TABLE IF NOT EXISTS role_permissions (
    id         BIGSERIAL PRIMARY KEY,

    role_id    BIGINT NOT NULL,
    module_id  BIGINT NOT NULL,
    action_id  BIGINT NOT NULL,

    allowed    BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_prp_role
        FOREIGN KEY (role_id) REFERENCES roles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_prp_module
        FOREIGN KEY (module_id) REFERENCES modules(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_prp_action
        FOREIGN KEY (action_id) REFERENCES actions(id)
        ON DELETE CASCADE,

    -- Prevent duplicate mappings:
    CONSTRAINT uk_prp_role_module_action
        UNIQUE (role_id, module_id, action_id)
);

-- Index for permission checks
CREATE INDEX IF NOT EXISTS idx_prp_lookup
ON role_permissions (role_id, module_id, action_id);

-- Faster checks when you filter allowed=true
CREATE INDEX IF NOT EXISTS idx_prp_lookup_allowed_true
ON role_permissions (role_id, module_id, action_id)
WHERE allowed = TRUE;

