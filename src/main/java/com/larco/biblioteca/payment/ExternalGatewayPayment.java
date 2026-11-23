package com.larco.biblioteca.payment;

import com.larco.biblioteca.resilience.CircuitBreaker;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Pago por pasarela externa: pasa por el Circuit Breaker y luego por el Adapter. */
@Component
public class ExternalGatewayPayment implements FinePaymentMethod {
    private final PaymentGateway gateway;
    private final CircuitBreaker breaker;

    public ExternalGatewayPayment(PaymentGateway gateway, CircuitBreaker breaker) {
        this.gateway = gateway;
        this.breaker = breaker;
    }

    @Override public String code() { return "PASARELA"; }

    @Override
    public PaymentResult pay(BigDecimal amount, String reference) {
        return breaker.call(() -> gateway.pay(amount, reference));
    }
}
