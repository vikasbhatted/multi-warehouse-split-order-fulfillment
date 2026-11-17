package com.fashionhub.fulfillment.common;

public class EngineUnavailableException extends RuntimeException {
    public EngineUnavailableException(String message, Throwable cause) { super(message, cause); }
    public EngineUnavailableException(String message) { super(message); }
}
