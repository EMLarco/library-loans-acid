package com.larco.biblioteca.repository;

import java.sql.Connection;
import java.sql.SQLException;

/** ISP: operaciones de disponibilidad usadas DENTRO de la transacción del préstamo. */
public interface BookInventoryRepository {
    /** SELECT ... FOR UPDATE: bloquea la fila del libro hasta COMMIT/ROLLBACK. Devuelve null si no existe. */
    Boolean lockAndGetAvailability(Connection c, int bookId) throws SQLException;
    void setAvailability(Connection c, int bookId, boolean available) throws SQLException;
}
