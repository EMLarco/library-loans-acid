package com.larco.biblioteca.web;

import com.larco.biblioteca.model.Book;
import com.larco.biblioteca.service.BookService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Controller: solo traduce HTTP <-> servicio (SRP). */
@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService books;

    public BookController(BookService books) { this.books = books; }

    @GetMapping
    public List<Book> list(@RequestParam(defaultValue = "false") boolean includeInactive) { return books.list(includeInactive); }

    @GetMapping("/{id}")
    public Book get(@PathVariable int id) { return books.get(id); }

    @PostMapping
    public Book create(@RequestBody Book book) { return books.create(book); }

    @PutMapping("/{id}")
    public Book update(@PathVariable int id, @RequestBody Book book) { return books.update(id, book); }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable int id, @RequestParam(defaultValue = "false") boolean physical) {
        books.delete(id, physical);
        return Map.of("deleted", id, "physical", physical);
    }
}
