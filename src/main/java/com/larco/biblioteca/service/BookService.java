package com.larco.biblioteca.service;

import com.larco.biblioteca.model.Book;
import com.larco.biblioteca.model.BusinessException;
import com.larco.biblioteca.repository.BookCommandRepository;
import com.larco.biblioteca.repository.BookQueryRepository;
import com.larco.biblioteca.support.ActivityLog;
import com.larco.biblioteca.transaction.TransactionFailedException;
import org.springframework.stereotype.Service;

import java.util.List;

/** CRUD de libros: solo validación y reglas; el SQL vive en el repositorio (SRP). */
@Service
public class BookService {

    private final BookQueryRepository query;
    private final BookCommandRepository command;
    private final ActivityLog activity;

    public BookService(BookQueryRepository query, BookCommandRepository command, ActivityLog activity) {
        this.query = query;
        this.command = command;
        this.activity = activity;
    }

    public List<Book> list(boolean includeInactive) { return query.findAll(includeInactive); }

    public Book get(int id) {
        return query.findById(id).orElseThrow(() -> new BusinessException("El libro " + id + " no existe"));
    }

    public Book create(Book b) {
        validate(b);
        Book created = command.create(b);
        activity.add("CRUD", "INSERT libro #" + created.bookId() + " '" + created.title() + "'");
        return created;
    }

    public Book update(int id, Book b) {
        validate(b);
        Book updated = command.update(id, b);
        if (updated == null) throw new BusinessException("El libro " + id + " no existe");
        activity.add("CRUD", "UPDATE libro #" + id + " '" + updated.title() + "'");
        return updated;
    }

    public void delete(int id, boolean physical) {
        if (!physical) {
            if (!command.deactivate(id)) throw new BusinessException("El libro " + id + " no existe");
            activity.add("CRUD", "Eliminación LÓGICA libro #" + id + " (active = false)");
            return;
        }
        try {
            if (!command.delete(id)) throw new BusinessException("El libro " + id + " no existe");
            activity.add("CRUD", "Eliminación FÍSICA libro #" + id);
        } catch (TransactionFailedException e) {
            throw new BusinessException("No se puede eliminar físicamente: el libro tiene préstamos registrados (FK). Use eliminación lógica.");
        }
    }

    private void validate(Book b) {
        if (b == null || b.title() == null || b.title().isBlank()) throw new BusinessException("El título es obligatorio");
        if (b.authorId() == null) throw new BusinessException("El autor es obligatorio");
    }
}
