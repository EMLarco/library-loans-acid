package com.larco.biblioteca.web;

import com.larco.biblioteca.payment.PaymentGateway;
import com.larco.biblioteca.payment.PaymentResult;
import com.larco.biblioteca.resilience.CircuitBreaker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/circuit")
public class CircuitController {
    private final CircuitBreaker breaker;
    private final PaymentGateway gateway;

    public CircuitController(CircuitBreaker breaker, PaymentGateway gateway) {
        this.breaker = breaker;
        this.gateway = gateway;
    }

    @GetMapping
    public Map<String, Object> state() { return breaker.snapshot(); }

    /** Llamada de prueba ($0.25) a la pasarela externa a través del Circuit Breaker + Adapter. */
    @PostMapping("/test-call")
    public PaymentResult testCall() {
        return breaker.call(() -> gateway.pay(new BigDecimal("0.25"), "PRUEBA"));
    }
}
