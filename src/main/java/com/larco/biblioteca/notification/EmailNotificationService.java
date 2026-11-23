package com.larco.biblioteca.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Email SIMULADO: no usa un servidor real, guarda el mensaje en una bandeja de salida en memoria. */
public class EmailNotificationService implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger("NOTIFICACION");
    private final List<String> outbox = new CopyOnWriteArrayList<>();

    @Override public String channel() { return "EMAIL (simulado)"; }

    @Override
    public void send(String recipient, String message) {
        String mail = "To: " + recipient + " | Subject: Biblioteca | " + message;
        outbox.add(mail);
        log.info("[EMAIL] {}", mail);
    }

    public List<String> outbox() { return List.copyOf(outbox); }
}
