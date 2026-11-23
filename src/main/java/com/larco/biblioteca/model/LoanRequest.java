package com.larco.biblioteca.model;

/**
 * Datos de entrada del préstamo.
 * simulateError = true provoca un fallo DESPUÉS de insertar el préstamo y cambiar la disponibilidad (Atomicidad).
 * holdMillis mantiene la transacción abierta unos milisegundos (para hacer visible la concurrencia).
 */
public record LoanRequest(Integer memberId, Integer bookId, Integer days, boolean simulateError, long holdMillis) {
    public LoanRequest(Integer memberId, Integer bookId, Integer days) {
        this(memberId, bookId, days, false, 0);
    }
}
