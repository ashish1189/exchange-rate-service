package com.crewmeister.cmcodingchallenge.currency.api;

import com.crewmeister.cmcodingchallenge.currency.api.dto.ConversionResponse;
import com.crewmeister.cmcodingchallenge.currency.api.dto.CurrencyResponse;
import com.crewmeister.cmcodingchallenge.currency.api.dto.ExchangeRateResponse;
import com.crewmeister.cmcodingchallenge.currency.api.dto.PagedResponse;
import com.crewmeister.cmcodingchallenge.currency.service.ExchangeRateService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CurrencyController {

    private final ExchangeRateService exchangeRateService;

    public CurrencyController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    /**
     * Returns all available currency codes.
     * GET /api/currencies
     */
    @GetMapping("/currencies")
    public ResponseEntity<List<CurrencyResponse>> getCurrencies() {
        return ResponseEntity.ok(exchangeRateService.getAvailableCurrencies());
    }

    /**
     * Returns all EUR-FX exchange rates for all currencies across all available dates.
     * GET /api/exchange-rates
     */
    @GetMapping("/exchange-rates")
    public ResponseEntity<PagedResponse<ExchangeRateResponse>> getAllRates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(exchangeRateService.getAllRates(page, size));
    }

    /**
     * Returns the EUR-FX exchange rate for a specific currency on a specific date.
     * Falls back to the last business day if no rate exists for the requested date.
     * GET /api/exchange-rates/{currency}/{date}
     */
    @GetMapping("/exchange-rates/{currency}/{date}")
    public ResponseEntity<ExchangeRateResponse> getRateForCurrencyOnDate(
            @PathVariable String currency,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(exchangeRateService.getRateForCurrencyOnDate(currency, date));
    }

    /**
     * Converts a foreign currency amount to EUR on a specific date.
     * GET /api/exchange-rates/convert?currency=USD&amount=100&date=2025-01-06
     */
    @GetMapping("/exchange-rates/convert")
    public ResponseEntity<ConversionResponse> convertToEur(
            @RequestParam String currency,
            @RequestParam BigDecimal amount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(exchangeRateService.convertToEur(currency, amount, date));
    }
}
