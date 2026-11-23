package com.larco.biblioteca.proxy;

import com.larco.biblioteca.model.AccessDeniedException;
import com.larco.biblioteca.model.Caller;
import com.larco.biblioteca.model.Loan;
import com.larco.biblioteca.model.LoanRequest;
import com.larco.biblioteca.repository.AuditRepository;
import com.larco.biblioteca.service.LoanService;
import com.larco.biblioteca.support.ActivityLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * PATRÓN PROXY.
 *   Cliente        -> LoanController (solo conoce LoanService)
 *   Proxy          -> LoanServiceProxy (@Primary, es lo que recibe el cliente)
 *   Servicio real  -> LoanServiceImpl
 * Tareas: validar acceso por rol, registrar quién y cuándo, auditar en audit_log y medir el tiempo.
 */
@Service
@Primary
public class LoanServiceProxy implements LoanService {

    private static final Set<String> ALLOWED_ROLES = Set.of("BIBLIOTECARIO", "ADMIN");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LoanService real;
    private final AuditRepository audit;
    private final ActivityLog activity;

    public LoanServiceProxy(@Qualifier("loanServiceImpl") LoanService real, AuditRepository audit, ActivityLog activity) {
        this.real = real;
        this.audit = audit;
        this.activity = activity;
    }

    @Override
    public Loan borrow(LoanRequest request, Caller caller) {
        checkAccess("PRESTAMO", caller, "libro=" + request.bookId() + " socio=" + request.memberId());
        long t0 = System.currentTimeMillis();
        try {
            return real.borrow(request, caller);
        } finally {
            activity.add("PROXY", "Servicio real respondió en " + (System.currentTimeMillis() - t0) + " ms");
        }
    }

    @Override
    public Loan returnBook(int loanId, Caller caller) {
        checkAccess("DEVOLUCION", caller, "prestamo=" + loanId);
        return real.returnBook(loanId, caller);
    }

    private void checkAccess(String op, Caller caller, String details) {
        String when = LocalDateTime.now().format(FMT);
        activity.add("PROXY", "Intercepta " + op + " | usuario=" + caller.user() + " rol=" + caller.role() + " | " + when);
        if (!ALLOWED_ROLES.contains(caller.role())) {
            audit.save("ACCESO_DENEGADO_" + op, caller.user(), "rol=" + caller.role() + " " + details);
            activity.add("PROXY", "ACCESO DENEGADO: el rol " + caller.role() + " no puede ejecutar " + op + " (no llega al servicio real)");
            throw new AccessDeniedException("Acceso denegado: el rol " + caller.role() + " no puede ejecutar " + op);
        }
        audit.save("SOLICITUD_" + op, caller.user(), "rol=" + caller.role() + " " + details);
        activity.add("PROXY", "Acceso permitido -> delega al servicio real");
    }
}
