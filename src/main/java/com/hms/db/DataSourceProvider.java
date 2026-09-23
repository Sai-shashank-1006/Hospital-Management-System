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
