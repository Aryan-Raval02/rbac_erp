
-- ============================================================
-- V2__seed_master_data.sql
-- Copies master data (Roles, Modules, Actions) from PUBLIC
-- into the current TENANT schema.
-- ============================================================

-- 1. Copy ROLES (Preserving ID linkage)
INSERT INTO roles (id, name, description)
SELECT id, name, description
FROM public.roles
ON CONFLICT (name) DO NOTHING; -- Safety check

-- 2. Copy MODULES
INSERT INTO modules (id, name, description)
SELECT id, name, description
FROM public.modules
ON CONFLICT (name) DO NOTHING;

-- 3. Copy ACTIONS
INSERT INTO actions (id, name, description)
SELECT id, name, description
FROM public.actions
ON CONFLICT (name) DO NOTHING;

-- 4. Copy PERMISSIONS (Using the same IDs ensures mappings stay valid)
INSERT INTO role_permissions (role_id, module_id, action_id, allowed)
SELECT role_id, module_id, action_id, allowed
FROM public.role_permissions
ON CONFLICT (role_id, module_id, action_id) DO NOTHING;

-- ============================================================
-- 5. RESET SEQUENCES (Critical Step)
-- Because we manually inserted IDs, we must advance the sequences
-- so the next "created" record doesn't clash with existing IDs.
-- ============================================================

SELECT setval(pg_get_serial_sequence('roles', 'id'), COALESCE(MAX(id), 0) + 1, false)
FROM roles;

SELECT setval(pg_get_serial_sequence('modules', 'id'), COALESCE(MAX(id), 0) + 1, false)
FROM modules;

SELECT setval(pg_get_serial_sequence('actions', 'id'), COALESCE(MAX(id), 0) + 1, false)
FROM actions;

SELECT setval(pg_get_serial_sequence('role_permissions', 'id'), COALESCE(MAX(id), 0) + 1, false)
FROM role_permissions;