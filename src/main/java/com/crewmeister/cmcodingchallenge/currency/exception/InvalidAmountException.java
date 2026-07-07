package com.crewmeister.cmcodingchallenge.currency.exception;

import java.math.BigDecimal;

public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException(BigDecimal amount) {
        super("Amount must be zero or positive, got: " + amount.toPlainString());
    }
}
