package com.hms.db;

import com.hms.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Owns the single application-wide connection pool.
 *
 * <p>The pool is created lazily on first use and closed by
 * {@link com.hms.web.AppLifecycleListener} when the context shuts down, so that
 * redeploying the WAR does not leak MySQL connections.
 */
public final class DataSourceProvider {

    private static volatile HikariDataSource dataSource;

    private DataSourceProvider() {
    }

    public static DataSource get() {
        HikariDataSource local = dataSource;
        if (local == null) {
            synchronized (DataSourceProvider.class) {
                local = dataSource;
                if (local == null) {
                    local = build();
                    dataSource = local;
                }
            }
        }
        return local;
    }

    private static HikariDataSource build() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(AppConfig.get("db.url"));
        config.setUsername(AppConfig.get("db.username"));
        config.setPassword(AppConfig.get("db.password"));
        config.setDriverClassName(AppConfig.get("db.driver", "com.mysql.cj.jdbc.Driver"));

        config.setMaximumPoolSize(AppConfig.getInt("db.pool.maxSize", 10));
        config.setMinimumIdle(AppConfig.getInt("db.pool.minIdle", 2));
        config.setConnectionTimeout(AppConfig.getInt("db.pool.connectionTimeoutMs", 10_000));
        config.setPoolName("hms-pool");

        // Build the pool even when the database is unreachable, instead of throwing
        // from the constructor. Failing fast here would abort Tomcat's context
        // startup, and Tomcat does not retry a failed deployment: a database that
        // was briefly unavailable at deploy time would leave the application dead
        // until someone redeployed it by hand. Deferring the error to the first
        // real query means the app starts, serves /health, and reports the database
        // as DOWN -- which is what the pipeline's smoke test is there to catch.
        config.setInitializationFailTimeout(-1);

        return new HikariDataSource(config);
    }

    /** Closes the pool. Safe to call more than once. */
    public static synchronized void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }
}
