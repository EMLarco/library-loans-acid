package com.larco.biblioteca.model;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) { super(message); }
}
