package com.larco.biblioteca.payment;

import com.larco.biblioteca.model.BusinessException;
import com.larco.biblioteca.model.Caller;
import com.larco.biblioteca.model.Loan;
import com.larco.biblioteca.observer.EventPublisher;
import com.larco.biblioteca.observer.LibraryEvent;
import com.larco.biblioteca.repository.LoanRepository;
import com.larco.biblioteca.support.ActivityLog;
import com.larco.biblioteca.transaction.TransactionRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Pago de multas. Recibe TODOS los métodos de pago por inyección (OCP + DIP). */
@Service
public class FinePaymentService {

    private final Map<String, FinePaymentMethod> methods;
    private final TransactionRunner tx;
    private final LoanRepository loans;
    private final EventPublisher events;
    private final ActivityLog activity;

    public FinePaymentService(List<FinePaymentMethod> methods, TransactionRunner tx, LoanRepository loans,
                              EventPublisher events, ActivityLog activity) {
        this.methods = methods.stream().collect(Collectors.toMap(FinePaymentMethod::code, Function.identity()));
        this.tx = tx;
        this.loans = loans;
        this.events = events;
        this.activity = activity;
    }

    public List<String> availableMethods() { return methods.keySet().stream().sorted().toList(); }

    public PaymentResult payFine(int loanId, String methodCode, Caller caller) {
        FinePaymentMethod method = methods.get(methodCode == null ? "" : methodCode.toUpperCase());
        if (method == null) throw new BusinessException("Método de pago no soportado: " + methodCode + ". Use " + availableMethods());

        PaymentResult result = tx.inTransaction("PAGO_MULTA prestamo=" + loanId, c -> {
            Loan l = loans.lockById(c, loanId).orElseThrow(() -> new BusinessException("El préstamo " + loanId + " no existe"));
            if (l.fineAmount().compareTo(BigDecimal.ZERO) <= 0) throw new BusinessException("El préstamo " + loanId + " no tiene multa");
            if (Boolean.TRUE.equals(l.finePaid())) throw new BusinessException("La multa del préstamo " + loanId + " ya fue pagada");
            activity.add("PAGO", "Cobrando $" + l.fineAmount() + " con " + method.code());
            PaymentResult r = method.pay(l.fineAmount(), "L" + loanId);
            if (!r.approved()) throw new BusinessException("Pago rechazado: " + r.message());
            loans.markFinePaid(c, loanId);
            return r;
        });

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("loanId", loanId);
        data.put("method", result.method());
        data.put("reference", result.reference());
        events.publish(LibraryEvent.of("MULTA_PAGADA", caller.user(), data));
        return result;
    }
}
