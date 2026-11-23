package com.larco.biblioteca.observer;

import com.larco.biblioteca.notification.NotificationService;
import com.larco.biblioteca.support.ActivityLog;
import org.springframework.stereotype.Component;

/** Observador 2: notifica al socio usando el canal configurado (consola o email simulado). */
@Component
public class NotificationObserver implements LibraryObserver {
    private final NotificationService notifier;
    private final ActivityLog activity;

    public NotificationObserver(NotificationService notifier, ActivityLog activity) {
        this.notifier = notifier;
        this.activity = activity;
    }

    @Override public String name() { return "NotificationObserver"; }

    @Override
    public void onEvent(LibraryEvent e) {
        String to = String.valueOf(e.data().getOrDefault("memberName", "socio"));
        String msg = switch (e.type()) {
            case "PRESTAMO_CREADO" -> "Su préstamo de '" + e.data().get("bookTitle") + "' fue registrado. Devolver antes de " + e.data().get("dueDate");
            case "PRESTAMO_DEVUELTO" -> "Gracias por devolver '" + e.data().get("bookTitle") + "'. Multa: $" + e.data().get("fine");
            case "MULTA_PAGADA" -> "Pago de multa confirmado. Referencia " + e.data().get("reference");
            default -> "Evento " + e.type();
        };
        notifier.send(to, msg);
        activity.add("OBSERVER", "NotificationObserver -> notificación enviada por " + notifier.channel() + " a " + to);
    }
}
