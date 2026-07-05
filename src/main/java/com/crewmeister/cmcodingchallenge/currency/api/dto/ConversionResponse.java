package com.crewmeister.cmcodingchallenge.currency.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConversionResponse(
        String currency,
        BigDecimal amount,
        LocalDate requestedDate,
        LocalDate rateDate,
        BigDecimal rate,
        BigDecimal convertedAmountInEur
) {}
