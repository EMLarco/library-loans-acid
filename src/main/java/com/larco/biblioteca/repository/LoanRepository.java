package com.larco.biblioteca.repository;

import com.larco.biblioteca.model.Loan;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanRepository {
    List<Loan> findAll();
    int insert(Connection c, int bookId, int memberId, LocalDate checkout, LocalDate due) throws SQLException;
    Optional<Loan> lockById(Connection c, int loanId) throws SQLException;
    void markReturned(Connection c, int loanId, LocalDate returnDate, BigDecimal fine) throws SQLException;
    void markFinePaid(Connection c, int loanId) throws SQLException;
}
