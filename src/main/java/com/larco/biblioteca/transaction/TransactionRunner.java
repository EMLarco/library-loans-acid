package com.larco.biblioteca.transaction;

import java.sql.Connection;

/** Contrato para ejecutar un bloque de trabajo dentro de UNA transacción (DIP). */
public interface TransactionRunner {

    @FunctionalInterface
    interface Work<T> {
        T execute(Connection connection) throws Exception;
    }

    <T> T inTransaction(String name, Work<T> work);
}
