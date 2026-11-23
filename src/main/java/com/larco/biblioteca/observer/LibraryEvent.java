package com.larco.biblioteca.observer;

import java.time.LocalDateTime;
import java.util.Map;

public record LibraryEvent(String type, String performedBy, Map<String, Object> data, LocalDateTime at) {
    public static LibraryEvent of(String type, String performedBy, Map<String, Object> data) {
        return new LibraryEvent(type, performedBy, data, LocalDateTime.now());
    }
}
