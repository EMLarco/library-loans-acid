package com.larco.biblioteca.payment;

import java.math.BigDecimal;

/**
 * ABIERTO/CERRADO: para agregar un nuevo método de pago se crea una nueva clase que implemente
 * esta interfaz (anotada con @Component). FinePaymentService NO se modifica.
 */
public interface FinePaymentMethod {
    String code();
    PaymentResult pay(BigDecimal amount, String reference);
}
