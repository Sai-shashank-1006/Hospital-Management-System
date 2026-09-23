package com.hms.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application settings, resolved in this order:
 *
 * <ol>
 *   <li>environment variable (e.g. {@code DB_URL} for key {@code db.url})</li>
 *   <li>JVM system property (e.g. {@code -Ddb.url=...})</li>
 *   <li>{@code application.properties} on the classpath</li>
 * </ol>
 *
 * The environment-variable layer is what lets the same WAR run unchanged against a
 * developer's local MySQL, the docker-compose stack, and the Jenkins deployment target.
 */
public final class AppConfig {

    private static final Properties DEFAULTS = load();

    private AppConfig() {
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read application.properties", e);
        }
        return props;
    }

    /** Converts a property key to its environment-variable form: {@code db.url} -> {@code DB_URL}. */
    static String envNameFor(String key) {
        return key.replace('.', '_')
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toUpperCase();
    }

    public static String get(String key) {
        String fromEnv = System.getenv(envNameFor(key));
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromSystem = System.getProperty(key);
        if (fromSystem != null && !fromSystem.isBlank()) {
            return fromSystem;
        }
        return DEFAULTS.getProperty(key);
    }

    public static String get(String key, String fallback) {
        String value = get(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    public static int getInt(String key, int fallback) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
