package com.larco.biblioteca.observer;

import com.larco.biblioteca.repository.AuditRepository;
import com.larco.biblioteca.support.ActivityLog;
import org.springframework.stereotype.Component;

/** Observador 1: guarda el evento en la tabla audit_log. */
@Component
public class AuditObserver implements LibraryObserver {
    private final AuditRepository audit;
    private final ActivityLog activity;

    public AuditObserver(AuditRepository audit, ActivityLog activity) {
        this.audit = audit;
        this.activity = activity;
    }

    @Override public String name() { return "AuditObserver"; }

    @Override
    public void onEvent(LibraryEvent e) {
        audit.save(e.type(), e.performedBy(), e.data().toString());
        activity.add("OBSERVER", "AuditObserver -> registro insertado en audit_log (" + e.type() + ")");
    }
}
