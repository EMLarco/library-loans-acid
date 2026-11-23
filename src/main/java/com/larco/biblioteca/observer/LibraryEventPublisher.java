package com.larco.biblioteca.observer;

import com.larco.biblioteca.support.ActivityLog;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Subject concreto. Spring inyecta todos los LibraryObserver existentes; agregar uno nuevo
 * no requiere modificar esta clase ni los servicios (Abierto/Cerrado).
 * Los eventos se publican DESPUÉS del COMMIT, para no notificar operaciones revertidas.
 */
@Component
public class LibraryEventPublisher implements EventPublisher {

    private final List<LibraryObserver> observers = new CopyOnWriteArrayList<>();
    private final ActivityLog activity;

    public LibraryEventPublisher(List<LibraryObserver> initialObservers, ActivityLog activity) {
        this.activity = activity;
        initialObservers.forEach(this::subscribe);
    }

    @Override
    public void subscribe(LibraryObserver observer) {
        observers.add(observer);
    }

    @Override
    public void publish(LibraryEvent event) {
        activity.add("OBSERVER", "Evento " + event.type() + " publicado -> " + observers.size() + " observadores");
        for (LibraryObserver o : observers) {
            try {
                o.onEvent(event);
            } catch (Exception e) {
                // Un observador que falla no afecta a los demás ni a la operación ya confirmada
                activity.add("OBSERVER", o.name() + " falló: " + e.getMessage());
            }
        }
    }
}
