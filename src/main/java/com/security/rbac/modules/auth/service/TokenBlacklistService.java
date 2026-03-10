package com.security.rbac.modules.auth.service;

import com.security.rbac.jwt.JwtService;
import com.security.rbac.modules.auth.entity.TokenBlacklist;
import com.security.rbac.modules.auth.repo.TokenBlacklistRepository;
import com.security.rbac.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtService jwtService;

    /**
     * Extracts expiration from token and saves it to the blacklist in the public
     * schema.
     */
    public void blacklistToken(String token) {
        if (token == null || token.isBlank())
            return;

        // Ensure we are operating in the public schema since this is a global table
        String previousTenant = TenantContext.getCurrentTenant();
        TenantContext.setCurrentTenant(TenantContext.DEFAULT_TENANT);

        try {
            if (tokenBlacklistRepository.existsByToken(token)) {
                log.debug("Token implies already blacklisted.");
                return;
            }

            Date expiration = jwtService.extractExpiration(token);
            if (expiration == null)
                return;

            TokenBlacklist blacklisted = TokenBlacklist.builder()
                    .token(token)
                    .expiresAt(expiration.toInstant())
                    .build();

            tokenBlacklistRepository.save(blacklisted);
            log.info("Token explicitly revoked. Expires at {}", expiration);
        } finally {
            if (previousTenant != null) {
                TenantContext.setCurrentTenant(previousTenant);
            } else {
                TenantContext.clear();
            }
        }
    }

    /**
     * Checks if a token is in the blacklist.
     */
    @Transactional(readOnly = true)
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank())
            return false;

        String previousTenant = TenantContext.getCurrentTenant();
        TenantContext.setCurrentTenant(TenantContext.DEFAULT_TENANT);
        try {
            return tokenBlacklistRepository.existsByToken(token);
        } finally {
            if (previousTenant != null) {
                TenantContext.setCurrentTenant(previousTenant);
            } else {
                TenantContext.clear();
            }
        }
    }

    /**
     * Runs periodically to delete expired tokens from the DB to prevent bloat.
     * Runs every hour.
     */
    @Scheduled(fixedRateString = "${app.jwt.blacklist-cleanup-rate:3600000}")
    @Transactional
    public void performCleanup() {
        TenantContext.setCurrentTenant(TenantContext.DEFAULT_TENANT);
        try {
            int deleted = tokenBlacklistRepository.deleteExpiredTokens(Instant.now());
            if (deleted > 0) {
                log.info("Cleaned up {} expired tokens from blacklist", deleted);
            }
        } finally {
            TenantContext.clear();
        }
    }
}
