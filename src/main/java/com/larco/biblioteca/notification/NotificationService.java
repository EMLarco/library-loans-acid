package com.larco.biblioteca.notification;

/** Contrato de notificación. Cualquier implementación debe poder sustituir a otra (Liskov). */
public interface NotificationService {
    String channel();
    void send(String recipient, String message);
}
