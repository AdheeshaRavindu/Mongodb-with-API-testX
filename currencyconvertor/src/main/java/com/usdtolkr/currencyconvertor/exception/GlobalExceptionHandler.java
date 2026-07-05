package com.usdtolkr.currencyconvertor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(UnauthorizedApiKeyException.class)
    public String handleUnauthorizedApiKey(UnauthorizedApiKeyException ex) {
        return ex.getMessage();
    }
}
