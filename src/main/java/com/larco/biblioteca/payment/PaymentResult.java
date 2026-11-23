package com.larco.biblioteca.payment;

public record PaymentResult(boolean approved, String method, String reference, String message) { }
