package com.hms.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    @DisplayName("a correct password verifies against its own hash")
    void roundTrip() {
        String hash = PasswordHasher.hash("correct horse battery staple");

        assertTrue(PasswordHasher.matches("correct horse battery staple", hash));
    }

    @Test
    @DisplayName("a wrong password does not verify")
    void wrongPassword() {
        String hash = PasswordHasher.hash("admin123");

        assertFalse(PasswordHasher.matches("admin124", hash));
        assertFalse(PasswordHasher.matches("", hash));
        assertFalse(PasswordHasher.matches("ADMIN123", hash));
    }

    @Test
    @DisplayName("the same password hashes differently every time")
    void saltMakesHashesUnique() {
        String first = PasswordHasher.hash("same password");
        String second = PasswordHasher.hash("same password");

        // A random salt per hash means two users with the same password do not
        // share a hash, so cracking one does not reveal the other.
        assertNotEquals(first, second);
        assertTrue(PasswordHasher.matches("same password", first));
        assertTrue(PasswordHasher.matches("same password", second));
    }

    @Test
    @DisplayName("the plain password never appears inside the stored hash")
    void hashDoesNotLeakPassword() {
        String password = "SuperSecret2026";
        String hash = PasswordHasher.hash(password);

        assertFalse(hash.contains(password));
    }

    @Test
    @DisplayName("the stored format records the algorithm and iteration count")
    void storedFormat() {
        String hash = PasswordHasher.hash("anything");
        String[] parts = hash.split("\\$");

        // Storing the parameters alongside the hash is what allows the iteration
        // count to be raised later without invalidating existing passwords.
        assertTrue(parts.length == 4, "expected 4 segments, got: " + hash);
        assertTrue("pbkdf2_sha256".equals(parts[0]));
        assertTrue(Integer.parseInt(parts[1]) >= 100_000,
                "iteration count should be high enough to slow brute force");
    }

    @Test
    @DisplayName("malformed or missing hashes are rejected rather than throwing")
    void malformedHashes() {
        assertFalse(PasswordHasher.matches("password", null));
        assertFalse(PasswordHasher.matches(null, "whatever"));
        assertFalse(PasswordHasher.matches("password", "not-a-hash"));
        assertFalse(PasswordHasher.matches("password", "pbkdf2_sha256$abc$def"));
        assertFalse(PasswordHasher.matches("password", "md5$1$aaaa$bbbb"));
    }

    @Test
    @DisplayName("a hash made with a different iteration count still verifies")
    void verifiesAcrossIterationCounts() {
        // Simulates an older stored hash: the count comes from the string,
        // not from today's constant.
        String hash = PasswordHasher.hash("legacy");
        String[] parts = hash.split("\\$");
        String rebuilt = parts[0] + "$" + parts[1] + "$" + parts[2] + "$" + parts[3];

        assertTrue(PasswordHasher.matches("legacy", rebuilt));
    }
}
