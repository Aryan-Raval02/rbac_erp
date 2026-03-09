CREATE TABLE IF NOT EXISTS public.role_permissions (
    id         BIGSERIAL PRIMARY KEY,

    role_id    BIGINT NOT NULL,
    module_id  BIGINT NOT NULL,
    action_id  BIGINT NOT NULL,

    allowed    BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_prp_role
        FOREIGN KEY (role_id) REFERENCES public.roles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_prp_module
        FOREIGN KEY (module_id) REFERENCES public.modules(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_prp_action
        FOREIGN KEY (action_id) REFERENCES public.actions(id)
        ON DELETE CASCADE,

    -- Prevent duplicate mappings:
    CONSTRAINT uk_prp_role_module_action
        UNIQUE (role_id, module_id, action_id)
);

-- Index for permission checks
CREATE INDEX IF NOT EXISTS idx_prp_lookup
ON public.role_permissions (role_id, module_id, action_id);

-- Faster checks when you filter allowed=true
CREATE INDEX IF NOT EXISTS idx_prp_lookup_allowed_true
ON public.role_permissions (role_id, module_id, action_id)
WHERE allowed = TRUE;