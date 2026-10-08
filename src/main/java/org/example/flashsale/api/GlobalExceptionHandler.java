package org.example.flashsale.api;

import org.example.flashsale.api.dto.ErrorResponse;
import org.example.flashsale.exception.SaleNotActiveException;
import org.example.flashsale.exception.SoldOutException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(IllegalArgumentException e) {
        return new ErrorResponse("NOT_FOUND", e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(SoldOutException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleSoldOut(SoldOutException e) {
        return new ErrorResponse("SOLD_OUT", e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(SaleNotActiveException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleNotActive(SaleNotActiveException e) {
        return new ErrorResponse("SALE_NOT_ACTIVE", e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception e) {
        return new ErrorResponse("INTERNAL_ERROR", e.getMessage(), LocalDateTime.now());
    }
}