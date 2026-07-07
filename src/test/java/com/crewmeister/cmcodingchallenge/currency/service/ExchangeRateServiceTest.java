package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.api.dto.ConversionResponse;
import com.crewmeister.cmcodingchallenge.currency.api.dto.CurrencyResponse;
import com.crewmeister.cmcodingchallenge.currency.api.dto.ExchangeRateResponse;
import com.crewmeister.cmcodingchallenge.currency.api.dto.PagedResponse;
import com.crewmeister.cmcodingchallenge.currency.exception.ExchangeRateNotFoundException;
import com.crewmeister.cmcodingchallenge.currency.repository.CurrencyRepository;
import com.crewmeister.cmcodingchallenge.currency.repository.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.currency.repository.entity.CurrencyEntity;
import com.crewmeister.cmcodingchallenge.currency.repository.entity.ExchangeRateEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock private CurrencyRepository currencyRepository;
    @Mock private ExchangeRateRepository exchangeRateRepository;
    @InjectMocks private ExchangeRateService service;

    // --- getAvailableCurrencies ---

    @Test
    void should_return_all_currency_codes_in_alphabetical_order() {
        when(currencyRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(
                new CurrencyEntity("EUR"),
                new CurrencyEntity("GBP"),
                new CurrencyEntity("USD")
        ));

        List<CurrencyResponse> result = service.getAvailableCurrencies();

        assertThat(result)
                .extracting(CurrencyResponse::currencyCode, CurrencyResponse::currencyName)
                .containsExactly(
                        tuple("EUR", Currency.getInstance("EUR").getDisplayName(Locale.ENGLISH)),
                        tuple("GBP", Currency.getInstance("GBP").getDisplayName(Locale.ENGLISH)),
                        tuple("USD", Currency.getInstance("USD").getDisplayName(Locale.ENGLISH))
                );
    }

    @Test
    void should_return_empty_list_when_no_currencies_in_database() {
        when(currencyRepository.findAllByOrderByCodeAsc()).thenReturn(List.of());

        List<CurrencyResponse> result = service.getAvailableCurrencies();

        assertThat(result).isEmpty();
    }

    // --- getAllRates ---

    @Test
    void should_return_paginated_exchange_rates_mapped_to_response_dto() {
        LocalDate date = LocalDate.of(2026, Month.JUNE, 15);
        PageRequest pageable = PageRequest.of(0, 25);
        Page<ExchangeRateEntity> page = new PageImpl<>(
                List.of(rateEntity("GBP", date, "0.8400"), rateEntity("USD", date, "1.0500")),
                pageable, 2);

        when(exchangeRateRepository.findAllOrderByCurrencyAndDate(pageable)).thenReturn(page);

        PagedResponse<ExchangeRateResponse> result = service.getAllRates(0, 25);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).currency()).isEqualTo("GBP");
        assertThat(result.content().get(0).date()).isEqualTo(date);
        assertThat(result.content().get(0).rate()).isEqualByComparingTo("0.8400");
        assertThat(result.content().get(1).currency()).isEqualTo("USD");
    }

    @Test
    void should_return_correct_pagination_metadata() {
        PageRequest pageable = PageRequest.of(2, 25);
        Page<ExchangeRateEntity> page = new PageImpl<>(List.of(), pageable, 120);

        when(exchangeRateRepository.findAllOrderByCurrencyAndDate(pageable)).thenReturn(page);

        PagedResponse<ExchangeRateResponse> result = service.getAllRates(2, 25);

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.pageSize()).isEqualTo(25);
        assertThat(result.totalElements()).isEqualTo(120);
        assertThat(result.totalPages()).isEqualTo(5);
        assertThat(result.first()).isFalse();
        assertThat(result.last()).isFalse();
    }

    @Test
    void should_return_empty_content_when_no_rates_in_database() {
        PageRequest pageable = PageRequest.of(0, 25);
        when(exchangeRateRepository.findAllOrderByCurrencyAndDate(pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PagedResponse<ExchangeRateResponse> result = service.getAllRates(0, 25);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue();
    }

    // --- getRateForCurrencyOnDate ---

    @Test
    void should_return_rate_for_currency_on_exact_requested_date() {
        LocalDate date = LocalDate.of(2026, Month.JUNE, 15);
        when(exchangeRateRepository.findLatestOnOrBefore("USD", date))
                .thenReturn(Optional.of(rateEntity("USD", date, "1.0500")));

        ExchangeRateResponse result = service.getRateForCurrencyOnDate("USD", date);

        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.date()).isEqualTo(date);
        assertThat(result.rate()).isEqualByComparingTo("1.0500");
    }

    @Test
    void should_return_last_available_rate_when_no_rate_exists_on_requested_date() {
        LocalDate saturday = LocalDate.of(2026, Month.JUNE, 13);
        LocalDate friday = LocalDate.of(2026, Month.JUNE, 12);
        when(exchangeRateRepository.findLatestOnOrBefore("USD", saturday))
                .thenReturn(Optional.of(rateEntity("USD", friday, "1.0500")));

        ExchangeRateResponse result = service.getRateForCurrencyOnDate("USD", saturday);

        assertThat(result.date()).isEqualTo(friday);
        assertThat(result.rate()).isEqualByComparingTo("1.0500");
    }

    @Test
    void should_throw_when_no_rate_found_for_currency_on_or_before_requested_date() {
        LocalDate date = LocalDate.of(2026, Month.JUNE, 15);
        when(exchangeRateRepository.findLatestOnOrBefore("USD", date))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRateForCurrencyOnDate("USD", date))
                .isInstanceOf(ExchangeRateNotFoundException.class)
                .hasMessageContaining("USD")
                .hasMessageContaining(date.toString());
    }

    // --- convertToEur ---

    @Test
    void should_convert_foreign_amount_to_eur_using_rate_on_requested_date() {
        LocalDate date = LocalDate.of(2026, Month.JUNE, 15);
        // rate 2.0 means 1 EUR = 2 USD, so 100 USD = 50 EUR
        when(exchangeRateRepository.findLatestOnOrBefore("USD", date))
                .thenReturn(Optional.of(rateEntity("USD", date, "2.000000")));

        ConversionResponse result = service.convertToEur("USD", new BigDecimal("100"), date);

        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.amount()).isEqualByComparingTo("100");
        assertThat(result.requestedDate()).isEqualTo(date);
        assertThat(result.rateDate()).isEqualTo(date);
        assertThat(result.rate()).isEqualByComparingTo("2.000000");
        assertThat(result.convertedAmountInEur()).isEqualByComparingTo("50");
    }

    @Test
    void should_reflect_fallback_rate_date_in_conversion_response_when_weekend_or_holiday() {
        LocalDate saturday = LocalDate.of(2026, Month.JUNE, 13);
        LocalDate friday = LocalDate.of(2026, Month.JUNE, 12);
        when(exchangeRateRepository.findLatestOnOrBefore("USD", saturday))
                .thenReturn(Optional.of(rateEntity("USD", friday, "2.000000")));

        ConversionResponse result = service.convertToEur("USD", new BigDecimal("100"), saturday);

        assertThat(result.requestedDate()).isEqualTo(saturday);
        assertThat(result.rateDate()).isEqualTo(friday);
        assertThat(result.convertedAmountInEur()).isEqualByComparingTo("50");
    }

    @Test
    void should_throw_when_no_rate_available_for_conversion() {
        LocalDate date = LocalDate.of(2026, Month.JUNE, 15);
        when(exchangeRateRepository.findLatestOnOrBefore("USD", date))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.convertToEur("USD", new BigDecimal("100"), date))
                .isInstanceOf(ExchangeRateNotFoundException.class)
                .hasMessageContaining("USD")
                .hasMessageContaining(date.toString());
    }

    // --- helpers ---

    private ExchangeRateEntity rateEntity(String code, LocalDate date, String rate) {
        return new ExchangeRateEntity(new CurrencyEntity(code), date, new BigDecimal(rate));
    }
}
