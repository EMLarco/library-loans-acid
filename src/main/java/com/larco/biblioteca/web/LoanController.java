package com.larco.biblioteca.web;

import com.larco.biblioteca.model.Caller;
import com.larco.biblioteca.model.Loan;
import com.larco.biblioteca.model.LoanRequest;
import com.larco.biblioteca.payment.FinePaymentService;
import com.larco.biblioteca.payment.PaymentResult;
import com.larco.biblioteca.repository.LoanRepository;
import com.larco.biblioteca.service.LoanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CLIENTE del Proxy: recibe un LoanService (Spring inyecta el @Primary = LoanServiceProxy).
 * No sabe que existe un proxy ni cuál es el servicio real.
 */
@RestController
@RequestMapping("/api/loans")
public class LoanController {
    private final LoanService loanService;
    private final LoanRepository loans;
    private final FinePaymentService payments;

    public LoanController(LoanService loanService, LoanRepository loans, FinePaymentService payments) {
        this.loanService = loanService;
        this.loans = loans;
        this.payments = payments;
    }

    @GetMapping
    public List<Loan> list() { return loans.findAll(); }

    @PostMapping
    public Loan borrow(@RequestBody LoanRequest request,
                       @RequestHeader(value = "X-User", required = false) String user,
                       @RequestHeader(value = "X-Role", required = false) String role) {
        return loanService.borrow(request, Caller.of(user, role));
    }

    @PostMapping("/{id}/return")
    public Loan returnBook(@PathVariable int id,
                           @RequestHeader(value = "X-User", required = false) String user,
                           @RequestHeader(value = "X-Role", required = false) String role) {
        return loanService.returnBook(id, Caller.of(user, role));
    }

    @GetMapping("/payment-methods")
    public List<String> methods() { return payments.availableMethods(); }

    @PostMapping("/{id}/pay-fine")
    public PaymentResult payFine(@PathVariable int id, @RequestParam String method,
                                 @RequestHeader(value = "X-User", required = false) String user,
                                 @RequestHeader(value = "X-Role", required = false) String role) {
        return payments.payFine(id, method, Caller.of(user, role));
    }
}
