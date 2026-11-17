package com.fashionhub.fulfillment.common;

/** Raised by the (mock) payment gateway when a refund cannot be processed. */
public class PaymentGatewayException extends RuntimeException {
    public PaymentGatewayException(String message) { super(message); }
}
