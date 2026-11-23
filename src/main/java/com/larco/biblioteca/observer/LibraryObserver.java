package com.larco.biblioteca.observer;

/** Observer: cualquier componente que quiera reaccionar a eventos de la biblioteca. */
public interface LibraryObserver {
    String name();
    void onEvent(LibraryEvent event);
}
