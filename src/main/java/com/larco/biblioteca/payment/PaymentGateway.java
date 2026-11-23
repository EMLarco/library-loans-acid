package com.larco.biblioteca.payment;

import java.math.BigDecimal;

/** Interfaz que USA la aplicación: payment.pay(amount). (Target del patrón Adapter) */
public interface PaymentGateway {
    PaymentResult pay(BigDecimal amount, String reference);
}
