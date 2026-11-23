package com.larco.biblioteca.resilience;

public class CircuitBreakerOpenException extends RuntimeException {
    public CircuitBreakerOpenException(String message) { super(message); }
}
