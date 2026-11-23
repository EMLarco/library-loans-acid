package com.larco.biblioteca.repository;

import com.larco.biblioteca.model.Book;

import java.util.List;
import java.util.Optional;

/** ISP: solo lectura de libros (lo que necesitan las pantallas de consulta). */
public interface BookQueryRepository {
    List<Book> findAll(boolean includeInactive);
    Optional<Book> findById(int bookId);
}
