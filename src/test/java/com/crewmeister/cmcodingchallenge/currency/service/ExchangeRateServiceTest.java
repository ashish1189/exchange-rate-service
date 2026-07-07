package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.api.dto.ConversionResponse;
import com.crewmeister.cmcodingchallenge.currency.api.dto.ExchangeRateResponse;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        List<String> result = service.getAvailableCurrencies();

        assertThat(result).containsExactly("EUR", "GBP", "USD");
    }

    @Test
    void should_return_empty_list_when_no_currencies_in_database() {
        when(currencyRepository.findAllByOrderByCodeAsc()).thenReturn(List.of());

        List<String> result = service.getAvailableCurrencies();

        assertThat(result).isEmpty();
    }

    // --- getAllRates ---

    @Test
    void should_return_all_exchange_rates_mapped_to_response_dto() {
        LocalDate date = LocalDate.of(2026, 1, 2);
        when(exchangeRateRepository.findAllOrderByCurrencyAndDate()).thenReturn(List.of(
                rateEntity("GBP", date, "0.8400"),
                rateEntity("USD", date, "1.0500")
        ));

        List<ExchangeRateResponse> result = service.getAllRates();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).currency()).isEqualTo("GBP");
        assertThat(result.get(0).date()).isEqualTo(date);
        assertThat(result.get(0).rate()).isEqualByComparingTo("0.8400");
        assertThat(result.get(1).currency()).isEqualTo("USD");
    }

    @Test
    void should_return_empty_list_when_no_rates_in_database() {
        when(exchangeRateRepository.findAllOrderByCurrencyAndDate()).thenReturn(List.of());

        List<ExchangeRateResponse> result = service.getAllRates();

        assertThat(result).isEmpty();
    }

    // --- getRateForCurrencyOnDate ---

    @Test
    void should_return_rate_for_currency_on_exact_requested_date() {
        LocalDate date = LocalDate.of(2026, 1, 2);
        when(exchangeRateRepository.findLatestOnOrBefore("USD", date))
                .thenReturn(Optional.of(rateEntity("USD", date, "1.0500")));

        ExchangeRateResponse result = service.getRateForCurrencyOnDate("USD", date);

        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.date()).isEqualTo(date);
        assertThat(result.rate()).isEqualByComparingTo("1.0500");
    }

    @Test
    void should_return_last_available_rate_when_no_rate_exists_on_requested_date() {
        LocalDate saturday = LocalDate.of(2026, 1, 3);
        LocalDate friday = LocalDate.of(2026, 1, 2);
        when(exchangeRateRepository.findLatestOnOrBefore("USD", saturday))
                .thenReturn(Optional.of(rateEntity("USD", friday, "1.0500")));

        ExchangeRateResponse result = service.getRateForCurrencyOnDate("USD", saturday);

        assertThat(result.date()).isEqualTo(friday);
        assertThat(result.rate()).isEqualByComparingTo("1.0500");
    }

    @Test
    void should_throw_when_no_rate_found_for_currency_on_or_before_requested_date() {
        LocalDate date = LocalDate.of(2026, 1, 2);
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
        LocalDate date = LocalDate.of(2026, 1, 2);
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
        LocalDate saturday = LocalDate.of(2026, 1, 3);
        LocalDate friday = LocalDate.of(2026, 1, 2);
        when(exchangeRateRepository.findLatestOnOrBefore("USD", saturday))
                .thenReturn(Optional.of(rateEntity("USD", friday, "2.000000")));

        ConversionResponse result = service.convertToEur("USD", new BigDecimal("100"), saturday);

        assertThat(result.requestedDate()).isEqualTo(saturday);
        assertThat(result.rateDate()).isEqualTo(friday);
        assertThat(result.convertedAmountInEur()).isEqualByComparingTo("50");
    }

    @Test
    void should_throw_when_no_rate_available_for_conversion() {
        LocalDate date = LocalDate.of(2026, 1, 2);
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
