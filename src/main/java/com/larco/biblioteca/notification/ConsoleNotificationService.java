package com.larco.biblioteca.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleNotificationService implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger("NOTIFICACION");

    @Override public String channel() { return "CONSOLA"; }

    @Override
    public void send(String recipient, String message) {
        log.info("[CONSOLA] Para {}: {}", recipient, message);
    }
}
