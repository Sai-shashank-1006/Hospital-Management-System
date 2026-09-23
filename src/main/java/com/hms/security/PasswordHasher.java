package com.hms.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Salted, iterated password hashing using PBKDF2-HMAC-SHA256.
 *
 * <p>Stored format: {@code pbkdf2_sha256$<iterations>$<salt-b64>$<hash-b64>}. Keeping
 * the parameters inside the string means the iteration count can be raised later
 * without invalidating existing passwords — an old hash still verifies with the count
 * it was created with.
 *
 * <p>PBKDF2 is used because it ships with the JDK. A dedicated password hash such as
 * bcrypt or Argon2 would resist GPU attacks better, but would add a third-party
 * dependency; PBKDF2 with a high iteration count is a reasonable, standards-based
 * choice and is far better than an unsalted digest.
 *
 * <p>Passwords are never stored or logged in plain text anywhere in this application.
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2_sha256";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String plainPassword) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);

        byte[] key = derive(plainPassword, salt, ITERATIONS);

        return PREFIX + "$" + ITERATIONS
                + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(key);
    }

    /** Verifies a candidate password. Returns false for malformed or missing hashes. */
    public static boolean matches(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }

        String[] parts = storedHash.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);

            byte[] actual = derive(plainPassword, salt, iterations);

            // Constant-time comparison: a length-dependent or early-exit compare
            // leaks information about the stored hash through response timing.
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing is unavailable", e);
        } finally {
            // Clear the copy of the password held inside the spec.
            spec.clearPassword();
        }
    }
}
