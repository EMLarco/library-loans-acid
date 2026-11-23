package com.larco.biblioteca.external;

import java.util.Map;

/**
 * Interfaz del PROVEEDOR EXTERNO (Adaptee). Es distinta a la de la aplicación:
 *   - el monto va en CENTAVOS (long) y no en BigDecimal
 *   - se envía un "code" de comercio + referencia en un solo String
 *   - responde un mapa con resultCode "00" = aprobado
 */
public interface LegacyBankApi {
    Map<String, Object> executeTransaction(long valueInCents, String code);
}
