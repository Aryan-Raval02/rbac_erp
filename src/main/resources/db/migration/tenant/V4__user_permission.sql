
-- ── User-Specific Permission Overrides ─────────────────────────
CREATE TABLE IF NOT EXISTS user_permissions (
    id          BIGSERIAL PRIMARY KEY,

    user_id     BIGINT NOT NULL,
    module_id   BIGINT NOT NULL,
    action_id   BIGINT NOT NULL,

    allowed     BOOLEAN NOT NULL,

    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_up_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_up_module
        FOREIGN KEY (module_id) REFERENCES modules(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_up_action
        FOREIGN KEY (action_id) REFERENCES actions(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_up_user_module_action
        UNIQUE (user_id, module_id, action_id)
);

CREATE INDEX IF NOT EXISTS idx_up_lookup
ON user_permissions (user_id, module_id, action_id);

CREATE INDEX IF NOT EXISTS idx_up_lookup_allowed_true
ON user_permissions (user_id, module_id, action_id)
WHERE allowed = TRUE;