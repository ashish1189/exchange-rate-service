package com.crewmeister.cmcodingchallenge.currency.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRate(Currency currency, LocalDate date, BigDecimal rate) {}
