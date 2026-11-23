package com.larco.biblioteca.service;

import com.larco.biblioteca.model.BusinessException;
import com.larco.biblioteca.model.Caller;
import com.larco.biblioteca.model.Loan;
import com.larco.biblioteca.model.LoanRequest;
import com.larco.biblioteca.observer.EventPublisher;
import com.larco.biblioteca.observer.LibraryEvent;
import com.larco.biblioteca.repository.BookInventoryRepository;
import com.larco.biblioteca.repository.LoanRepository;
import com.larco.biblioteca.repository.MemberRepository;
import com.larco.biblioteca.support.ActivityLog;
import com.larco.biblioteca.transaction.TransactionRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SERVICIO REAL (RealSubject del Proxy). Operación principal: PRÉSTAMO DE LIBRO.
 *   1. Verificar socio  2. Bloquear libro y verificar disponibilidad  3. Registrar préstamo
 *   4. Cambiar disponibilidad  5. COMMIT  (si algo falla -> ROLLBACK)
 * Depende solo de interfaces (DIP).
 */
@Service("loanServiceImpl")
public class LoanServiceImpl implements LoanService {

    static final BigDecimal FINE_PER_DAY = new BigDecimal("0.50");

    private final TransactionRunner tx;
    private final MemberRepository members;
    private final BookInventoryRepository inventory;
    private final LoanRepository loans;
    private final EventPublisher events;
    private final ActivityLog activity;

    public LoanServiceImpl(TransactionRunner tx, MemberRepository members, BookInventoryRepository inventory,
                           LoanRepository loans, EventPublisher events, ActivityLog activity) {
        this.tx = tx;
        this.members = members;
        this.inventory = inventory;
        this.loans = loans;
        this.events = events;
        this.activity = activity;
    }

    @Override
    public Loan borrow(LoanRequest r, Caller caller) {
        if (r.memberId() == null || r.bookId() == null) throw new BusinessException("Socio y libro son obligatorios");
        if (r.days() == null || r.days() <= 0) throw new BusinessException("Los días de préstamo deben ser mayores que cero");

        String name = "PRESTAMO libro=" + r.bookId() + " socio=" + r.memberId();
        int loanId = tx.inTransaction(name, c -> {
            if (!members.exists(c, r.memberId())) throw new BusinessException("El socio " + r.memberId() + " no existe");
            activity.add("PRESTAMO", "Paso 1: socio " + r.memberId() + " verificado");

            Boolean available = inventory.lockAndGetAvailability(c, r.bookId());
            activity.add("PRESTAMO", "Paso 2: fila del libro " + r.bookId() + " bloqueada (SELECT ... FOR UPDATE)");
            if (available == null) throw new BusinessException("El libro " + r.bookId() + " no existe o está inactivo");
            if (!available) throw new BusinessException("El libro " + r.bookId() + " NO está disponible (ya prestado)");

            if (r.holdMillis() > 0) Thread.sleep(r.holdMillis());

            LocalDate today = LocalDate.now();
            int id = loans.insert(c, r.bookId(), r.memberId(), today, today.plusDays(r.days()));
            activity.add("PRESTAMO", "Paso 3: préstamo #" + id + " insertado en loans");

            inventory.setAvailability(c, r.bookId(), false);
            activity.add("PRESTAMO", "Paso 4: libro " + r.bookId() + " marcado como NO disponible");

            if (r.simulateError()) {
                activity.add("PRESTAMO", "Paso 5: *** ERROR SIMULADO *** antes del COMMIT");
                throw new SimulatedFailureException("ERROR SIMULADO después de registrar el préstamo #" + id);
            }
            return id;
        });

        Loan loan = findLoan(loanId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("loanId", loan.loanId());
        data.put("bookTitle", loan.bookTitle());
        data.put("memberName", loan.memberName());
        data.put("dueDate", loan.dueDate());
        events.publish(LibraryEvent.of("PRESTAMO_CREADO", caller.user(), data));
        return loan;
    }

    @Override
    public Loan returnBook(int loanId, Caller caller) {
        BigDecimal fine = tx.inTransaction("DEVOLUCION prestamo=" + loanId, c -> {
            Loan l = loans.lockById(c, loanId).orElseThrow(() -> new BusinessException("El préstamo " + loanId + " no existe"));
            if (Boolean.TRUE.equals(l.returnStatus())) throw new BusinessException("El préstamo " + loanId + " ya fue devuelto");
            LocalDate today = LocalDate.now();
            long late = Math.max(0, ChronoUnit.DAYS.between(l.dueDate(), today));
            BigDecimal f = FINE_PER_DAY.multiply(BigDecimal.valueOf(late));
            loans.markReturned(c, loanId, today, f);
            inventory.setAvailability(c, l.bookId(), true);
            activity.add("DEVOLUCION", "Préstamo #" + loanId + " devuelto, libro " + l.bookId() + " disponible, multa $" + f);
            return f;
        });
        Loan loan = findLoan(loanId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("loanId", loanId);
        data.put("bookTitle", loan.bookTitle());
        data.put("memberName", loan.memberName());
        data.put("fine", fine);
        events.publish(LibraryEvent.of("PRESTAMO_DEVUELTO", caller.user(), data));
        return loan;
    }

    private Loan findLoan(int id) {
        return loans.findAll().stream().filter(l -> l.loanId() == id).findFirst()
                .orElseThrow(() -> new BusinessException("Préstamo " + id + " no encontrado"));
    }
}
