package com.larco.biblioteca.repository.postgres;

import com.larco.biblioteca.model.Book;
import com.larco.biblioteca.repository.BookCommandRepository;
import com.larco.biblioteca.repository.BookInventoryRepository;
import com.larco.biblioteca.repository.BookQueryRepository;
import com.larco.biblioteca.transaction.TransactionFailedException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Detalle concreto (PostgreSQL). Los servicios dependen de las interfaces, no de esta clase (DIP). */
@Repository
public class PostgresBookRepository implements BookQueryRepository, BookCommandRepository, BookInventoryRepository {

    private static final String SELECT = """
            SELECT b.book_id, b.title, b.author_id, a.name AS author_name, b.isbn, b.publication_date,
                   b.genre_id, b.availability_status, b.active
              FROM books b JOIN authors a ON a.author_id = b.author_id
            """;

    private final DataSource ds;

    public PostgresBookRepository(DataSource ds) { this.ds = ds; }

    @Override
    public List<Book> findAll(boolean includeInactive) {
        String sql = SELECT + (includeInactive ? "" : " WHERE b.active = TRUE") + " ORDER BY b.book_id";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Book> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw wrap(e); }
    }

    @Override
    public Optional<Book> findById(int bookId) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(SELECT + " WHERE b.book_id = ?")) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw wrap(e); }
    }

    @Override
    public Book create(Book b) {
        String sql = "INSERT INTO books (title, author_id, isbn, publication_date, genre_id, availability_status, active) "
                + "VALUES (?,?,?,?,?,TRUE,TRUE) RETURNING book_id";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            fill(ps, b);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return findById(rs.getInt(1)).orElseThrow(); }
        } catch (SQLException e) { throw wrap(e); }
    }

    @Override
    public Book update(int id, Book b) {
        String sql = "UPDATE books SET title=?, author_id=?, isbn=?, publication_date=?, genre_id=? WHERE book_id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            fill(ps, b);
            ps.setInt(6, id);
            if (ps.executeUpdate() == 0) return null;
            return findById(id).orElseThrow();
        } catch (SQLException e) { throw wrap(e); }
    }

    @Override
    public boolean deactivate(int id) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE books SET active = FALSE WHERE book_id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw wrap(e); }
    }

    @Override
    public boolean delete(int id) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM books WHERE book_id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw wrap(e); }
    }

    @Override
    public Boolean lockAndGetAvailability(Connection c, int bookId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT availability_status FROM books WHERE book_id = ? AND active = TRUE FOR UPDATE")) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getBoolean(1) : null; }
        }
    }

    @Override
    public void setAvailability(Connection c, int bookId, boolean available) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE books SET availability_status = ? WHERE book_id = ?")) {
            ps.setBoolean(1, available);
            ps.setInt(2, bookId);
            ps.executeUpdate();
        }
    }

    private void fill(PreparedStatement ps, Book b) throws SQLException {
        ps.setString(1, b.title());
        if (b.authorId() == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, b.authorId());
        ps.setString(3, b.isbn());
        ps.setDate(4, Jdbc.sql(b.publicationDate()));
        if (b.genreId() == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, b.genreId());
    }

    private Book map(ResultSet rs) throws SQLException {
        Integer genre = rs.getObject("genre_id") == null ? null : rs.getInt("genre_id");
        return new Book(rs.getInt("book_id"), rs.getString("title"), rs.getInt("author_id"), rs.getString("author_name"),
                rs.getString("isbn"), Jdbc.date(rs.getDate("publication_date")), genre,
                rs.getBoolean("availability_status"), rs.getBoolean("active"));
    }

    private RuntimeException wrap(SQLException e) {
        return new TransactionFailedException(e.getMessage(), e);
    }
}
