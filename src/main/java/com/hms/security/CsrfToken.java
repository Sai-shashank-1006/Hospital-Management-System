package com.hms.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Per-session token defending state-changing requests against cross-site request forgery.
 *
 * <p>Without this, a page on another site could POST to this application using the
 * browser's session cookie — deleting a patient, say — because the browser attaches
 * the cookie automatically. Requiring a token that the attacker's page cannot read
 * (it is on our session, and same-origin policy blocks reading our pages) closes that.
 */
public final class CsrfToken {

    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfToken() {
    }

    public static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Constant-time comparison, so a mismatch reveals nothing through timing. */
    public static boolean matches(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }
}
