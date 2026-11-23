package com.larco.biblioteca.web;

import com.larco.biblioteca.model.Author;
import com.larco.biblioteca.model.Member;
import com.larco.biblioteca.repository.AuthorRepository;
import com.larco.biblioteca.repository.MemberRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final AuthorRepository authors;
    private final MemberRepository members;

    public CatalogController(AuthorRepository authors, MemberRepository members) {
        this.authors = authors;
        this.members = members;
    }

    @GetMapping("/authors")
    public List<Author> authors() { return authors.findAll(); }

    @GetMapping("/members")
    public List<Member> members() { return members.findAll(); }
}
