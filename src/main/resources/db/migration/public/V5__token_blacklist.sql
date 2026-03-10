-- public.token_blacklist table to store revoked JWTs for all users uniformly
CREATE TABLE IF NOT EXISTS public.token_blacklist (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token VARCHAR(1000) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index on expires_at to efficiently clean up expired tokens periodically
CREATE INDEX IF NOT EXISTS idx_token_blacklist_expires_at ON public.token_blacklist(expires_at);

-- Index on token for fast lookup during authentication filter
CREATE INDEX IF NOT EXISTS idx_token_blacklist_token ON public.token_blacklist(token);
