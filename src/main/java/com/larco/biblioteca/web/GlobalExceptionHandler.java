package com.larco.biblioteca.web;

import com.larco.biblioteca.external.ExternalServiceException;
import com.larco.biblioteca.model.AccessDeniedException;
import com.larco.biblioteca.model.BusinessException;
import com.larco.biblioteca.resilience.CircuitBreakerOpenException;
import com.larco.biblioteca.service.SimulatedFailureException;
import com.larco.biblioteca.transaction.TransactionFailedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Manejo controlado de errores: cada tipo de fallo devuelve un código HTTP y un mensaje claro. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> business(BusinessException e) { return body(409, "REGLA_DE_NEGOCIO", e); }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> denied(AccessDeniedException e) { return body(403, "ACCESO_DENEGADO", e); }

    @ExceptionHandler(SimulatedFailureException.class)
    public ResponseEntity<Map<String, Object>> simulated(SimulatedFailureException e) { return body(500, "ERROR_SIMULADO_ROLLBACK", e); }

    @ExceptionHandler(CircuitBreakerOpenException.class)
    public ResponseEntity<Map<String, Object>> open(CircuitBreakerOpenException e) { return body(503, "CIRCUIT_BREAKER_OPEN", e); }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<Map<String, Object>> external(ExternalServiceException e) { return body(502, "SERVICIO_EXTERNO_FALLO", e); }

    @ExceptionHandler(TransactionFailedException.class)
    public ResponseEntity<Map<String, Object>> db(TransactionFailedException e) { return body(400, "RESTRICCION_BD", e); }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> other(Exception e) { return body(500, "ERROR", e); }

    private ResponseEntity<Map<String, Object>> body(int status, String type, Exception e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", status);
        m.put("error", type);
        m.put("message", e.getMessage());
        return ResponseEntity.status(status).body(m);
    }
}
