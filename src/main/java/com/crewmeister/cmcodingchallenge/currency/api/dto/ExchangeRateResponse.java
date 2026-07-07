package com.crewmeister.cmcodingchallenge.currency.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateResponse(String currency, LocalDate date, BigDecimal rate) {}
