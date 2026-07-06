package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.client.BundesbankApiClient;
import com.crewmeister.cmcodingchallenge.currency.domain.Currency;
import com.crewmeister.cmcodingchallenge.currency.domain.ExchangeRate;
import com.crewmeister.cmcodingchallenge.currency.repository.CurrencyRepository;
import com.crewmeister.cmcodingchallenge.currency.repository.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.currency.repository.entity.CurrencyEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateIngestionServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-06T10:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDate TODAY = LocalDate.now(FIXED_CLOCK);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);

    @Mock private BundesbankApiClient apiClient;
    @Mock private CurrencyRepository currencyRepository;
    @Mock private ExchangeRateRepository exchangeRateRepository;

    private ExchangeRateIngestionService service() {
        return new ExchangeRateIngestionService(apiClient, currencyRepository, exchangeRateRepository, FIXED_CLOCK);
    }

    // --- loadHistoricalRatesIfEmpty ---

    @Test
    void should_skip_bulk_load_when_database_already_has_rates() {
        when(exchangeRateRepository.existsBy()).thenReturn(true);

        service().loadHistoricalRatesIfEmpty();

        verify(apiClient, never()).fetchAllRates();
        verify(exchangeRateRepository, never()).save(any());
    }

    @Test
    void should_fetch_and_persist_all_historical_rates_when_database_is_empty() {
        ExchangeRate usdRate = rate("USD", "2026-01-02", "1.0500");
        CurrencyEntity usdEntity = new CurrencyEntity("USD");

        when(exchangeRateRepository.existsBy()).thenReturn(false);
        when(apiClient.fetchAllRates()).thenReturn(List.of(usdRate));
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usdEntity));
        when(exchangeRateRepository.existsByCurrencyCodeAndDate("USD", usdRate.date())).thenReturn(false);

        service().loadHistoricalRatesIfEmpty();

        verify(apiClient).fetchAllRates();
        verify(exchangeRateRepository).save(argThat(e ->
                e.getCurrency().getCode().equals("USD") &&
                e.getDate().equals(usdRate.date()) &&
                e.getRate().compareTo(usdRate.rate()) == 0));
    }

    @Test
    void should_create_new_currency_entity_when_not_yet_in_database() {
        ExchangeRate gbpRate = rate("GBP", "2026-01-02", "0.8400");
        CurrencyEntity savedGbp = new CurrencyEntity("GBP");

        when(exchangeRateRepository.existsBy()).thenReturn(false);
        when(apiClient.fetchAllRates()).thenReturn(List.of(gbpRate));
        when(currencyRepository.findByCode("GBP")).thenReturn(Optional.empty());
        when(currencyRepository.save(any())).thenReturn(savedGbp);
        when(exchangeRateRepository.existsByCurrencyCodeAndDate("GBP", gbpRate.date())).thenReturn(false);

        service().loadHistoricalRatesIfEmpty();

        verify(currencyRepository).save(argThat(e -> e.getCode().equals("GBP")));
    }

    @Test
    void should_reuse_existing_currency_entity_without_inserting_it_again() {
        ExchangeRate rate = rate("USD", "2026-01-02", "1.0500");
        CurrencyEntity existing = new CurrencyEntity("USD");

        when(exchangeRateRepository.existsBy()).thenReturn(false);
        when(apiClient.fetchAllRates()).thenReturn(List.of(rate));
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(existing));
        when(exchangeRateRepository.existsByCurrencyCodeAndDate("USD", rate.date())).thenReturn(false);

        service().loadHistoricalRatesIfEmpty();

        verify(currencyRepository, never()).save(any());
    }

    @Test
    void should_skip_rate_if_entry_already_exists_for_currency_and_date() {
        ExchangeRate rate = rate("USD", "2026-01-02", "1.0500");
        CurrencyEntity usdEntity = new CurrencyEntity("USD");

        when(exchangeRateRepository.existsBy()).thenReturn(false);
        when(apiClient.fetchAllRates()).thenReturn(List.of(rate));
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usdEntity));
        when(exchangeRateRepository.existsByCurrencyCodeAndDate("USD", rate.date())).thenReturn(true);

        service().loadHistoricalRatesIfEmpty();

        verify(exchangeRateRepository, never()).save(any());
    }

    // --- fetchAndStoreDailyRates ---

    @Test
    void should_fetch_yesterdays_rates_on_daily_scheduled_run() {
        ExchangeRate rate = rate("USD", YESTERDAY.toString(), "1.0480");
        CurrencyEntity usdEntity = new CurrencyEntity("USD");

        when(apiClient.fetchRatesForDate(YESTERDAY)).thenReturn(List.of(rate));
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usdEntity));
        when(exchangeRateRepository.existsByCurrencyCodeAndDate("USD", rate.date())).thenReturn(false);

        service().fetchAndStoreDailyRates();

        verify(apiClient).fetchRatesForDate(YESTERDAY);
        verify(exchangeRateRepository).save(any());
    }

    private static ExchangeRate rate(String code, String date, String value) {
        return new ExchangeRate(new Currency(code), LocalDate.parse(date), new BigDecimal(value));
    }
}
