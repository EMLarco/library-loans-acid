package com.larco.biblioteca.service;

public class SimulatedFailureException extends RuntimeException {
    public SimulatedFailureException(String message) { super(message); }
}
