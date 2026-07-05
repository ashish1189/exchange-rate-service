package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.api.dto.ConversionResponse;
import com.crewmeister.cmcodingchallenge.currency.api.dto.ExchangeRateResponse;
import com.crewmeister.cmcodingchallenge.currency.repository.ExchangeRateRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExchangeRateService {

    private final ExchangeRateRepository repository;

    public ExchangeRateService(ExchangeRateRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns the list of all available currency codes.
     */
    public List<String> getAvailableCurrencies() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Returns all EUR-FX exchange rates across all currencies and all available dates.
     */
    public List<ExchangeRateResponse> getAllRates() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Returns the EUR-FX rate for a given currency on a given date.
     * Falls back to the last available business day if no rate exists for that date.
     */
    public ExchangeRateResponse getRateForCurrencyOnDate(String currency, LocalDate date) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Converts a foreign currency amount to EUR on a given date.
     * Falls back to the last available business day rate with full metadata in the response.
     */
    public ConversionResponse convertToEur(String currency, BigDecimal amount, LocalDate date) {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
