package com.larco.biblioteca.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Al iniciar: si la tabla books no existe ejecuta el esquema y los datos de ejemplo.
 * Si ya existe NO toca nada (así se puede demostrar la Durabilidad tras reiniciar).
 */
@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            boolean exists;
            try (ResultSet rs = st.executeQuery("SELECT to_regclass('public.books') IS NOT NULL")) {
                rs.next();
                exists = rs.getBoolean(1);
            }
            if (exists) {
                log.info("Esquema existente detectado: se conservan los datos (Durabilidad)");
                return;
            }
            log.info("Base vacía: creando esquema y datos de ejemplo");
            st.execute(read("db/01_schema.sql"));
            st.execute(read("db/02_sample_data.sql"));
            log.info("Esquema y datos de ejemplo cargados");
        }
    }

    private String read(String path) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("No se encontró " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
