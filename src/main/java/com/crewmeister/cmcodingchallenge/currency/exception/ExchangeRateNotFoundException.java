package com.crewmeister.cmcodingchallenge.currency.exception;

import java.time.LocalDate;

public class ExchangeRateNotFoundException extends RuntimeException {

    public ExchangeRateNotFoundException(String currencyCode, LocalDate date) {
        super("No exchange rate found for currency '" + currencyCode + "' on or before " + date);
    }
}
