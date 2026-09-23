package com.hms.support;

import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds a throwaway in-memory database for a single test class.
 *
 * <p>H2 runs in MySQL compatibility mode so the production SQL in the DAOs is exercised
 * as written, without needing a MySQL server on the build agent. Each call gets its own
 * named database so tests stay isolated from one another.
 */
public final class TestDatabase {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private TestDatabase() {
    }

    public static DataSource create() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:hms_test_" + COUNTER.incrementAndGet()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        applySchema(ds);
        return ds;
    }

    private static void applySchema(DataSource ds) {
        String script = readResource("schema-h2.sql");

        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            for (String statement : script.split(";")) {
                String sql = stripComments(statement).trim();
                if (!sql.isEmpty()) {
                    st.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not apply the test schema", e);
        }
    }

    /** Drops whole-line "--" comments, which would otherwise swallow the statement after them. */
    private static String stripComments(String sql) {
        StringBuilder out = new StringBuilder();
        for (String line : sql.split("\n")) {
            if (!line.trim().startsWith("--")) {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }

    private static String readResource(String name) {
        try (InputStream in = TestDatabase.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read test resource: " + name, e);
        }
    }
}
