package com.larco.biblioteca.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Loan(Integer loanId, Integer bookId, String bookTitle, Integer memberId, String memberName,
                   LocalDate checkoutDate, LocalDate dueDate, LocalDate returnDate, Boolean returnStatus,
                   BigDecimal fineAmount, Boolean finePaid) { }
