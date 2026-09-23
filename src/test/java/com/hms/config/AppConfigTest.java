package com.hms.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @AfterEach
    void clearOverrides() {
        System.clearProperty("db.url");
        System.clearProperty("db.pool.maxSize");
    }

    @Test
    @DisplayName("property keys map onto the environment variable names used by Docker and Jenkins")
    void envNameMapping() {
        assertEquals("DB_URL", AppConfig.envNameFor("db.url"));
        assertEquals("DB_USERNAME", AppConfig.envNameFor("db.username"));
        assertEquals("DB_POOL_MAX_SIZE", AppConfig.envNameFor("db.pool.maxSize"));
    }

    @Test
    @DisplayName("values fall back to application.properties when nothing overrides them")
    void readsDefaultsFromClasspath() {
        String url = AppConfig.get("db.url");

        assertNotNull(url, "db.url should be present in application.properties");
        assertTrue(url.startsWith("jdbc:mysql://"), "expected a MySQL JDBC URL, got: " + url);
    }

    @Test
    @DisplayName("a system property overrides the packaged default")
    void systemPropertyWins() {
        System.setProperty("db.url", "jdbc:mysql://example:3306/other");

        assertEquals("jdbc:mysql://example:3306/other", AppConfig.get("db.url"));
    }

    @Test
    @DisplayName("missing or unparseable numbers fall back instead of throwing")
    void numericFallbacks() {
        assertEquals(42, AppConfig.getInt("db.pool.doesNotExist", 42));

        System.setProperty("db.pool.maxSize", "not-a-number");
        assertEquals(7, AppConfig.getInt("db.pool.maxSize", 7));
    }

    @Test
    @DisplayName("the string fallback applies to unknown keys")
    void stringFallback() {
        assertEquals("fallback", AppConfig.get("no.such.key", "fallback"));
    }
}
