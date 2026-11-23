package com.larco.biblioteca.service;

import com.larco.biblioteca.model.Caller;
import com.larco.biblioteca.model.Loan;
import com.larco.biblioteca.model.LoanRequest;

/** Contrato de la operación principal de negocio. Lo implementan el servicio real y el Proxy. */
public interface LoanService {
    Loan borrow(LoanRequest request, Caller caller);
    Loan returnBook(int loanId, Caller caller);
}
