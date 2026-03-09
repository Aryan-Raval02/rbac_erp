package com.security.rbac.modules.ceo.dto.response;

import java.time.Instant;

/**
 * Response returned after a successful CEO sign-up.
 */
public record SignUpResponse(

        /** Newly created global user id. */
        Long userId,

        /** Full name of the registered CEO. */
        String fullName,

        /** Login email. */
        String email,

        /** Login username. */
        String username,

        /** Assigned system role (CEO). */
        String systemRole,

        /** When the account was created. */
        Instant createdAt,

        String message) {
}
