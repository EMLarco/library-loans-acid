package com.larco.biblioteca.adapter;

import com.larco.biblioteca.external.LegacyBankApi;
import com.larco.biblioteca.payment.PaymentGateway;
import com.larco.biblioteca.payment.PaymentResult;
import com.larco.biblioteca.support.ActivityLog;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * PATRÓN ADAPTER.
 *   Aplicación  -> PaymentGateway.pay(amount, reference)
 *   Adapter     -> LegacyBankPaymentAdapter (convierte)
 *   Externo     -> LegacyBankApi.executeTransaction(valueInCents, code)
 * Transformaciones: BigDecimal dólares -> long centavos; referencia -> code "BIBLIO|ref";
 * resultCode "00" -> PaymentResult(approved=true).
 */
@Component
public class LegacyBankPaymentAdapter implements PaymentGateway {

    private static final String MERCHANT = "BIBLIO";
    private final LegacyBankApi external;
    private final ActivityLog activity;

    public LegacyBankPaymentAdapter(LegacyBankApi external, ActivityLog activity) {
        this.external = external;
        this.activity = activity;
    }

    @Override
    public PaymentResult pay(BigDecimal amount, String reference) {
        long cents = amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        String code = MERCHANT + "|" + reference;
        activity.add("ADAPTER", "pay(" + amount + ", " + reference + ") -> executeTransaction(" + cents + ", \"" + code + "\")");
        Map<String, Object> response = external.executeTransaction(cents, code);
        boolean ok = "00".equals(String.valueOf(response.get("resultCode")));
        PaymentResult result = new PaymentResult(ok, "PASARELA", String.valueOf(response.get("authNumber")),
                ok ? "Aprobado por Banco Legacy" : "Rechazado código " + response.get("resultCode"));
        activity.add("ADAPTER", "respuesta " + response + " -> " + result);
        return result;
    }
}
