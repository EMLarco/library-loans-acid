package com.larco.biblioteca.repository.postgres;

import com.larco.biblioteca.model.Loan;
import com.larco.biblioteca.repository.LoanRepository;
import com.larco.biblioteca.transaction.TransactionFailedException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PostgresLoanRepository implements LoanRepository {

    private static final String SELECT = """
            SELECT l.loan_id, l.book_id, b.title, l.member_id, m.name, l.checkout_date, l.due_date, l.return_date,
                   l.return_status, l.fine_amount, l.fine_paid
              FROM loans l
              JOIN books b   ON b.book_id = l.book_id
              JOIN members m ON m.member_id = l.member_id
            """;

    private final DataSource ds;

    public PostgresLoanRepository(DataSource ds) { this.ds = ds; }

    @Override
    public List<Loan> findAll() {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + " ORDER BY l.loan_id DESC");
             ResultSet rs = ps.executeQuery()) {
            List<Loan> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new TransactionFailedException(e.getMessage(), e); }
    }

    @Override
    public int insert(Connection c, int bookId, int memberId, LocalDate checkout, LocalDate due) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO loans (book_id, member_id, checkout_date, due_date) VALUES (?,?,?,?) RETURNING loan_id")) {
            ps.setInt(1, bookId);
            ps.setInt(2, memberId);
            ps.setDate(3, Jdbc.sql(checkout));
            ps.setDate(4, Jdbc.sql(due));
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    @Override
    public Optional<Loan> lockById(Connection c, int loanId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE l.loan_id = ? FOR UPDATE OF l")) {
            ps.setInt(1, loanId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        }
    }

    @Override
    public void markReturned(Connection c, int loanId, LocalDate returnDate, BigDecimal fine) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE loans SET return_status = TRUE, return_date = ?, fine_amount = ? WHERE loan_id = ?")) {
            ps.setDate(1, Jdbc.sql(returnDate));
            ps.setBigDecimal(2, fine);
            ps.setInt(3, loanId);
            ps.executeUpdate();
        }
    }

    @Override
    public void markFinePaid(Connection c, int loanId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE loans SET fine_paid = TRUE WHERE loan_id = ?")) {
            ps.setInt(1, loanId);
            ps.executeUpdate();
        }
    }

    private Loan map(ResultSet rs) throws SQLException {
        return new Loan(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getInt(4), rs.getString(5),
                Jdbc.date(rs.getDate(6)), Jdbc.date(rs.getDate(7)), Jdbc.date(rs.getDate(8)),
                rs.getBoolean(9), rs.getBigDecimal(10), rs.getBoolean(11));
    }
}
