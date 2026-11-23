package com.larco.biblioteca.transaction;

import com.larco.biblioteca.support.ActivityLog;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Transacción REAL de PostgreSQL con JDBC: autoCommit=false -> COMMIT o ROLLBACK.
 * Nivel de aislamiento READ COMMITTED + bloqueo de fila (SELECT ... FOR UPDATE) en los repositorios.
 */
@Component
public class JdbcTransactionRunner implements TransactionRunner {

    private final DataSource dataSource;
    private final ActivityLog activity;

    public JdbcTransactionRunner(DataSource dataSource, ActivityLog activity) {
        this.dataSource = dataSource;
        this.activity = activity;
    }

    @Override
    public <T> T inTransaction(String name, Work<T> work) {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            activity.add("TX", "BEGIN  [" + name + "]");
            try {
                T result = work.execute(c);
                c.commit();
                activity.add("TX", "COMMIT [" + name + "]");
                return result;
            } catch (Exception e) {
                c.rollback();
                activity.add("TX", "ROLLBACK [" + name + "] causa: " + e.getMessage());
                if (e instanceof RuntimeException re) throw re;
                throw new TransactionFailedException(e.getMessage(), e);
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new TransactionFailedException("Error de conexión: " + e.getMessage(), e);
        }
    }
}
