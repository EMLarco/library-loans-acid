package com.larco.biblioteca.repository;

import com.larco.biblioteca.model.Author;

import java.util.List;

public interface AuthorRepository {
    List<Author> findAll();
}
