package com.usdtolkr.currencyconvertor.exception;

public class UnauthorizedApiKeyException extends RuntimeException {

    public UnauthorizedApiKeyException(String message) {
        super(message);
    }
}
