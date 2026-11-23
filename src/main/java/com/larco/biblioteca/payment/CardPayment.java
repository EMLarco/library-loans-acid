package com.larco.biblioteca.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CardPayment implements FinePaymentMethod {
    @Override public String code() { return "TARJETA"; }

    @Override
    public PaymentResult pay(BigDecimal amount, String reference) {
        return new PaymentResult(true, code(), "POS-" + reference, "Pago con tarjeta aprobado por el datáfono local");
    }
}
