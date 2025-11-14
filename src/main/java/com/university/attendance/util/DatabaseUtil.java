package com.university.attendance.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

@Slf4j
public class DatabaseUtil {
    private static HikariDataSource dataSource;
    private static final Properties config = new Properties();

    static {
        loadDatabaseConfig();
        initializeDataSource();
    }

    private static void loadDatabaseConfig() {
        try (InputStream input = DatabaseUtil.class.getClassLoader()
                .getResourceAsStream("config/database.properties")) {

            if (input == null) {
                throw new RuntimeException("Database configuration file not found");
            }

            config.load(input);
            log.info("Database configuration loaded successfully");

        } catch (IOException e) {
            log.error("Failed to load database configuration", e);
            throw new RuntimeException("Database configuration error", e);
        }
    }

    private static void initializeDataSource() {
        try {
            HikariConfig hikariConfig = new HikariConfig();

            hikariConfig.setJdbcUrl(config.getProperty("db.url"));
            hikariConfig.setUsername(config.getProperty("db.username"));
            hikariConfig.setPassword(config.getProperty("db.password"));
            hikariConfig.setDriverClassName(config.getProperty("db.driver"));

            int poolSize = Integer.parseInt(config.getProperty("db.pool.size", "10"));
            hikariConfig.setMaximumPoolSize(poolSize);
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setConnectionTimeout(30000);
            hikariConfig.setIdleTimeout(600000);
            hikariConfig.setMaxLifetime(1800000);

            hikariConfig.setPoolName("AttendanceSystemPool");
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(hikariConfig);

            log.info("Database connection pool initialized successfully");

        } catch (Exception e) {
            log.error("Failed to initialize database connection pool", e);
            throw new RuntimeException("Database initialization error", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database connection pool not initialized");
        }

        Connection connection = dataSource.getConnection();
        log.debug("Database connection established");
        return connection;
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                log.debug("Database connection closed");
            } catch (SQLException e) {
                log.warn("Error closing database connection", e);
            }
        }
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("Database connection pool shutdown");
        }
    }

    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            log.error("Database connection test failed", e);
            return false;
        }
    }
}