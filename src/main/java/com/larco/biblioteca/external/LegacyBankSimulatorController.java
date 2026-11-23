package com.larco.biblioteca.external;

import com.larco.biblioteca.support.ActivityLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SERVICIO EXTERNO SIMULADO ("Banco Legacy"). Se ejecuta localmente y solo se consume por HTTP.
 * Se puede poner en modo FALLA para probar el Circuit Breaker.
 */
@RestController
@RequestMapping("/external/legacy-bank")
public class LegacyBankSimulatorController {

    private final AtomicBoolean failing = new AtomicBoolean(false);
    private final AtomicInteger received = new AtomicInteger();
    private final ActivityLog activity;

    public LegacyBankSimulatorController(ActivityLog activity) { this.activity = activity; }

    @PostMapping("/executeTransaction")
    public ResponseEntity<Map<String, Object>> executeTransaction(@RequestParam long value, @RequestParam String code) {
        int n = received.incrementAndGet();
        Map<String, Object> body = new LinkedHashMap<>();
        if (failing.get()) {
            activity.add("SERV.EXTERNO", "Solicitud #" + n + " recibida -> responde 503 (modo FALLA)");
            body.put("resultCode", "96");
            body.put("error", "SYSTEM_MALFUNCTION");
            return ResponseEntity.status(503).body(body);
        }
        String auth = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        activity.add("SERV.EXTERNO", "Solicitud #" + n + " executeTransaction(value=" + value + ", code=" + code + ") -> 00 APROBADO");
        body.put("resultCode", "00");
        body.put("authNumber", auth);
        body.put("valueInCents", value);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/mode")
    public Map<String, Object> setMode(@RequestParam boolean fail) {
        failing.set(fail);
        activity.add("SERV.EXTERNO", fail ? "*** Servicio externo CAÍDO (modo FALLA) ***" : "*** Servicio externo RECUPERADO ***");
        return status();
    }

    @GetMapping("/mode")
    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("failing", failing.get());
        m.put("requestsReceived", received.get());
        return m;
    }
}
