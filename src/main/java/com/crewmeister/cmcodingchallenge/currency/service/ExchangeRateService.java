package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.api.dto.ConversionResponse;
import com.crewmeister.cmcodingchallenge.currency.api.dto.CurrencyResponse;
import com.crewmeister.cmcodingchallenge.currency.api.dto.ExchangeRateResponse;
import com.crewmeister.cmcodingchallenge.currency.api.dto.PagedResponse;
import com.crewmeister.cmcodingchallenge.currency.domain.ExchangeRate;
import com.crewmeister.cmcodingchallenge.currency.exception.ExchangeRateNotFoundException;
import com.crewmeister.cmcodingchallenge.currency.exception.InvalidAmountException;
import com.crewmeister.cmcodingchallenge.currency.repository.CurrencyRepository;
import com.crewmeister.cmcodingchallenge.currency.repository.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.currency.repository.entity.CurrencyEntity;
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

    public PagedResponse<ExchangeRateResponse> getAllRates(int page, int size) {
        return PagedResponse.from(
                exchangeRateRepository.findAllOrderByCurrencyAndDate(PageRequest.of(page, size))
                        .map(ExchangeRateMapper::toDomain)
                        .map(this::toRateResponse)
        );
    }

    @Cacheable(value = "rates", key = "#currency.toUpperCase() + '_' + #date")
    public ExchangeRateResponse getRateForCurrencyOnDate(String currency, LocalDate date) {
        ExchangeRate rate = findRateOrThrow(currency.toUpperCase(), date);
        return toRateResponse(rate);
    }

    public ConversionResponse convertToEur(String currency, BigDecimal amount, LocalDate date) {
        if (amount.signum() < 0) {
            throw new InvalidAmountException(amount);
        }
        ExchangeRate rate = findRateOrThrow(currency.toUpperCase(), date);
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
