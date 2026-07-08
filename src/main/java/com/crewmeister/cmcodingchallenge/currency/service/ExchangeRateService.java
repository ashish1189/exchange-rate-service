package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.api.dto.ConversionResponse;
import com.crewmeister.cmcodingchallenge.currency.api.dto.CurrencyResponse;
import com.crewmeister.cmcodingchallenge.currency.api.dto.ExchangeRateResponse;
import com.crewmeister.cmcodingchallenge.currency.api.dto.ExchangeRatePageResponse;
import com.crewmeister.cmcodingchallenge.currency.domain.ExchangeRate;
import com.crewmeister.cmcodingchallenge.currency.exception.ExchangeRateNotFoundException;
import com.crewmeister.cmcodingchallenge.currency.exception.InvalidAmountException;
import com.crewmeister.cmcodingchallenge.currency.repository.CurrencyRepository;
import com.crewmeister.cmcodingchallenge.currency.repository.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.currency.repository.mapper.ExchangeRateMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

@Service
public class ExchangeRateService {

    private static final int CONVERSION_SCALE = 6;

    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeRateService(CurrencyRepository currencyRepository,
                               ExchangeRateRepository exchangeRateRepository) {
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    /**
     * Cached: the currency list changes only when new currencies are ingested (rare).
     * java.util.Currency provides ISO 4217 display names from the JDK - no extra API call or DB column needed.
     *
     * @return List of CurrencyResponse objects containing currency codes and display names.
     */
    @Cacheable("currencies")
    public List<CurrencyResponse> getAvailableCurrencies() {
        return currencyRepository.findAllByOrderByCodeAsc()
                .stream()
                .map(entity -> {
                    String code = entity.getCode();
                    String name = Currency.getInstance(code).getDisplayName(Locale.ENGLISH);
                    return new CurrencyResponse(code, name);
                })
                .toList();
    }

    public ExchangeRatePageResponse<ExchangeRateResponse> getAllRates(int page, int size) {
        return ExchangeRatePageResponse.from(
                exchangeRateRepository.findAllOrderByCurrencyAndDate(PageRequest.of(page, size))
                        .map(ExchangeRateMapper::toDomain)
                        .map(this::toRateResponse)
        );
    }

    /**
     * Cached: a rate for a given currency and date is immutable once published by the Bundesbank.
     * Key includes toUpperCase() so "usd" and "USD" map to the same cache entry.
     *
     * @param currency
     *        Currency code (case-insensitive) to look up.
     * @param date
     *        Date to look up the rate for. If no rate exists for this date, the most recent prior rate is returned (e.g. for weekends/holidays).
     * @return ExchangeRateResponse containing the rate for the given currency and date.
     * @throws ExchangeRateNotFoundException if no rate exists for the given currency and date (or any prior date).
     */
    @Cacheable(value = "rates", key = "#currency.toUpperCase() + '_' + #date")
    public ExchangeRateResponse getRateForCurrencyOnDate(String currency, LocalDate date) {
        ExchangeRate rate = findRateOrThrow(currency.toUpperCase(), date);
        return toRateResponse(rate);
    }

    /**
     * Not cached: the result depends on caller-supplied amount which has unbounded variation,
     * making cache hit rate negligible. getRateForCurrencyOnDate (which IS cached) is called internally.
     *
     * @param currency
     *       Currency code (case-insensitive) to convert from.
     * @param amount
     *      Amount in the given currency to convert to EUR. Must be non-negative.
     * @param date
     *     Date to look up the rate for. If no rate exists for this date, the most recent prior rate is used (e.g. for weekends/holidays).
     * @return ConversionResponse containing the original amount, the rate used, and the converted amount in EUR.
     */
    public ConversionResponse convertToEur(String currency, BigDecimal amount, LocalDate date) {
        if (amount.signum() < 0) {
            throw new InvalidAmountException(amount);
        }
        ExchangeRate rate = findRateOrThrow(currency.toUpperCase(), date);
        // Zero short-circuits division to avoid ArithmeticException and unnecessary computation.
        BigDecimal converted = amount.signum() == 0
                ? BigDecimal.ZERO
                : amount.divide(rate.rate(), CONVERSION_SCALE, RoundingMode.HALF_UP);
        return new ConversionResponse(currency, amount, date, rate.date(), rate.rate(), converted);
    }

    private ExchangeRate findRateOrThrow(String currency, LocalDate date) {
        return exchangeRateRepository.findLatestOnOrBefore(currency, date)
                .map(ExchangeRateMapper::toDomain)
                .orElseThrow(() -> new ExchangeRateNotFoundException(currency, date));
    }

    private ExchangeRateResponse toRateResponse(ExchangeRate rate) {
        return new ExchangeRateResponse(rate.currency().code(), rate.date(), rate.rate());
    }
}
