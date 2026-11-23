package com.larco.biblioteca.observer;

/** Sujeto (Subject) del patrón Observer. */
public interface EventPublisher {
    void subscribe(LibraryObserver observer);
    void publish(LibraryEvent event);
}
