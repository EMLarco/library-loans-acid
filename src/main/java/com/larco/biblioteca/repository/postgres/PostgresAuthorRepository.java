package com.larco.biblioteca.repository.postgres;

import com.larco.biblioteca.model.Author;
import com.larco.biblioteca.repository.AuthorRepository;
import com.larco.biblioteca.transaction.TransactionFailedException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PostgresAuthorRepository implements AuthorRepository {
    private final DataSource ds;

    public PostgresAuthorRepository(DataSource ds) { this.ds = ds; }

    @Override
    public List<Author> findAll() {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT author_id, name, bio FROM authors ORDER BY author_id");
             ResultSet rs = ps.executeQuery()) {
            List<Author> out = new ArrayList<>();
            while (rs.next()) out.add(new Author(rs.getInt(1), rs.getString(2), rs.getString(3)));
            return out;
        } catch (SQLException e) { throw new TransactionFailedException(e.getMessage(), e); }
    }
}
