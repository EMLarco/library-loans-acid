package com.larco.biblioteca.transaction;

public class TransactionFailedException extends RuntimeException {
    public TransactionFailedException(String message, Throwable cause) { super(message, cause); }
}
