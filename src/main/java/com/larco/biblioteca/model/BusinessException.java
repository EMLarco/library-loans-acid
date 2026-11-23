package com.larco.biblioteca.model;

/** Error de negocio controlado (se responde con HTTP 409/400 y mensaje claro). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
