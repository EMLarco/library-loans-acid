package com.larco.biblioteca.model;

import java.time.LocalDate;

/** Entidad principal del CRUD. authorName es solo de lectura (JOIN con authors). */
public record Book(Integer bookId, String title, Integer authorId, String authorName, String isbn,
                   LocalDate publicationDate, Integer genreId, Boolean availabilityStatus, Boolean active) { }
