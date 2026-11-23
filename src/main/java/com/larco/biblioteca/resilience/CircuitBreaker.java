package com.larco.biblioteca.resilience;

import com.larco.biblioteca.support.ActivityLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * PATRÓN CIRCUIT BREAKER (implementación propia).
 *   CLOSED    : las llamadas pasan; cada fallo suma. Al llegar al umbral -> OPEN.
 *   OPEN      : NO se llama al servicio externo (falla rápido) hasta que pase openSeconds.
 *   HALF_OPEN : se permite UNA llamada de prueba. Éxito -> CLOSED. Fallo -> OPEN otra vez.
 */
@Component
public class CircuitBreaker {

    private final int failureThreshold;
    private final Duration openDuration;
    private final ActivityLog activity;

    private CircuitState state = CircuitState.CLOSED;
    private int consecutiveFailures = 0;
    private Instant openedAt;
    private boolean trialInProgress = false;

    public CircuitBreaker(@Value("${app.circuit.failure-threshold}") int failureThreshold,
                          @Value("${app.circuit.open-seconds}") int openSeconds,
                          ActivityLog activity) {
        this.failureThreshold = failureThreshold;
        this.openDuration = Duration.ofSeconds(openSeconds);
        this.activity = activity;
    }

    public <T> T call(Supplier<T> action) {
        beforeCall();
        try {
            T result = action.get();
            onSuccess();
            return result;
        } catch (RuntimeException e) {
            onFailure(e);
            throw e;
        }
    }

    private synchronized void beforeCall() {
        if (state == CircuitState.OPEN) {
            long elapsed = Duration.between(openedAt, Instant.now()).toSeconds();
            if (Duration.between(openedAt, Instant.now()).compareTo(openDuration) >= 0) {
                transition(CircuitState.HALF_OPEN, "pasaron " + openDuration.toSeconds() + " s; se permite una llamada de prueba");
            } else {
                activity.add("CIRCUIT", "OPEN -> solicitud BLOQUEADA sin llamar al servicio externo (faltan "
                        + (openDuration.toSeconds() - elapsed) + " s)");
                throw new CircuitBreakerOpenException("Circuito ABIERTO: servicio externo temporalmente deshabilitado");
            }
        }
        if (state == CircuitState.HALF_OPEN) {
            if (trialInProgress) throw new CircuitBreakerOpenException("Circuito HALF-OPEN: ya hay una llamada de prueba en curso");
            trialInProgress = true;
            activity.add("CIRCUIT", "HALF_OPEN -> ejecutando llamada de prueba");
        }
    }

    private synchronized void onSuccess() {
        trialInProgress = false;
        if (state == CircuitState.HALF_OPEN) {
            transition(CircuitState.CLOSED, "la llamada de prueba funcionó");
        } else {
            activity.add("CIRCUIT", "CLOSED -> llamada exitosa");
        }
        consecutiveFailures = 0;
    }

    private synchronized void onFailure(RuntimeException e) {
        trialInProgress = false;
        if (e instanceof CircuitBreakerOpenException) return;
        consecutiveFailures++;
        if (state == CircuitState.HALF_OPEN) {
            openedAt = Instant.now();
            transition(CircuitState.OPEN, "la llamada de prueba falló (" + e.getMessage() + ")");
            return;
        }
        activity.add("CIRCUIT", "CLOSED -> fallo " + consecutiveFailures + "/" + failureThreshold + ": " + e.getMessage());
        if (consecutiveFailures >= failureThreshold) {
            openedAt = Instant.now();
            transition(CircuitState.OPEN, "se alcanzaron " + failureThreshold + " fallos consecutivos");
        }
    }

    private void transition(CircuitState to, String why) {
        activity.add("CIRCUIT", "*** " + state + " -> " + to + " *** (" + why + ")");
        state = to;
    }

    public synchronized Map<String, Object> snapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("state", state);
        m.put("consecutiveFailures", consecutiveFailures);
        m.put("failureThreshold", failureThreshold);
        m.put("openSeconds", openDuration.toSeconds());
        if (state == CircuitState.OPEN) {
            long left = openDuration.toSeconds() - Duration.between(openedAt, Instant.now()).toSeconds();
            m.put("secondsUntilHalfOpen", Math.max(0, left));
        }
        return m;
    }
}
