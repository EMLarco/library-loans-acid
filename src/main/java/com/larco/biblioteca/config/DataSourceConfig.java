package com.larco.biblioteca.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Crea el pool de conexiones. Si la base library_db no existe, la crea
 * conectándose primero a la base de mantenimiento "postgres".
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    public DataSource dataSource(@Value("${app.db.host}") String host,
                                 @Value("${app.db.port}") int port,
                                 @Value("${app.db.name}") String dbName,
                                 @Value("${app.db.user}") String user,
                                 @Value("${app.db.password}") String password) throws Exception {
        if (!dbName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Nombre de base inválido: " + dbName);
        }
        String adminUrl = "jdbc:postgresql://" + host + ":" + port + "/postgres";
        try (Connection c = DriverManager.getConnection(adminUrl, user, password);
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    try (Statement st = c.createStatement()) {
                        st.execute("CREATE DATABASE " + dbName);
                    }
                    log.info("Base de datos '{}' creada", dbName);
                }
            }
        }

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + dbName);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(10);
        cfg.setPoolName("biblioteca-pool");
        log.info("Conectando a PostgreSQL {}:{}/{} como {}", host, port, dbName, user);
        return new HikariDataSource(cfg);
    }
}
