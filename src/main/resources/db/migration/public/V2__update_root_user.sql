-- ============================================================
-- V2__update_root_user.sql
-- Aligns root_user table with the required schema.
-- Drops unused columns and adds 'name' column.
-- ============================================================

-- Remove old columns that are no longer needed
ALTER TABLE public.root_user
    DROP COLUMN IF EXISTS role_id,
    DROP COLUMN IF EXISTS is_active;

-- Add the columns the entity now expects
ALTER TABLE public.root_user
    ADD COLUMN IF NOT EXISTS name    VARCHAR(150),
    ADD COLUMN IF NOT EXISTS active  BOOLEAN NOT NULL DEFAULT TRUE;
