package org.example.flashsale.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class Database {

    private static final HikariDataSource dataSource = createDataSource();

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/flashsale");
        config.setUsername("postgres");
        config.setPassword("postgreswint");   // ⚠️ CHANGE THIS to your real password
        config.setMaximumPoolSize(50);    // up to 50 concurrent connections
        config.setMinimumIdle(5);
        config.setPoolName("flash-sale-pool");
        config.setConnectionTimeout(30_000);   // 30s to get a connection
        return new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void shutdown() {
        dataSource.close();
    }
}