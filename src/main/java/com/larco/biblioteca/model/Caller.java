package com.larco.biblioteca.model;

/** Quién ejecuta la operación (lo envía el cliente en las cabeceras X-User / X-Role). */
public record Caller(String user, String role) {
    public static Caller of(String user, String role) {
        return new Caller(user == null || user.isBlank() ? "anonimo" : user,
                          role == null || role.isBlank() ? "SIN_ROL" : role.toUpperCase());
    }
}
