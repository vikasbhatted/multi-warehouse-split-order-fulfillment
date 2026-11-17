package com.fashionhub.fulfillment.common;

/** Business rule violation raised while validating / allocating an order. */
public class OrderRejectedException extends RuntimeException {
    public OrderRejectedException(String message) { super(message); }
}
