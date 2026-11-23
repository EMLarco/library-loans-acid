package com.larco.biblioteca.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CashPayment implements FinePaymentMethod {
    @Override public String code() { return "EFECTIVO"; }

    @Override
    public PaymentResult pay(BigDecimal amount, String reference) {
        return new PaymentResult(true, code(), "CAJA-" + reference, "Pago en efectivo recibido en ventanilla");
    }
}
