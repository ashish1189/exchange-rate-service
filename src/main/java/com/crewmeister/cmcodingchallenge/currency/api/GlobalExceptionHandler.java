package com.crewmeister.cmcodingchallenge.currency.api;

import com.crewmeister.cmcodingchallenge.currency.exception.ExchangeRateNotFoundException;
import com.crewmeister.cmcodingchallenge.currency.exception.InvalidAmountException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ExchangeRateNotFoundException.class)
    ProblemDetail handleNotFound(ExchangeRateNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidAmountException.class)
    ProblemDetail handleInvalidAmount(InvalidAmountException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
