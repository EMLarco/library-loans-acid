package com.larco.biblioteca.repository;

import com.larco.biblioteca.model.Book;

/** ISP: escritura del CRUD de libros. */
public interface BookCommandRepository {
    Book create(Book book);
    Book update(int bookId, Book book);
    boolean deactivate(int bookId);   // eliminación lógica
    boolean delete(int bookId);       // eliminación física (solo si no tiene préstamos)
}
