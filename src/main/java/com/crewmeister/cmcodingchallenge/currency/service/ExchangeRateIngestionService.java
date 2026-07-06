package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.client.BundesbankApiClient;
import com.crewmeister.cmcodingchallenge.currency.domain.ExchangeRate;
import com.crewmeister.cmcodingchallenge.currency.repository.CurrencyRepository;
import com.crewmeister.cmcodingchallenge.currency.repository.ExchangeRateRepository;
import com.crewmeister.cmcodingchallenge.currency.repository.entity.CurrencyEntity;
import com.crewmeister.cmcodingchallenge.currency.repository.entity.ExchangeRateEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class ExchangeRateIngestionService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateIngestionService.class);

    private final BundesbankApiClient apiClient;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final Clock clock;

    public ExchangeRateIngestionService(BundesbankApiClient apiClient,
                                        CurrencyRepository currencyRepository,
                                        ExchangeRateRepository exchangeRateRepository,
                                        Clock clock) {
        this.apiClient = apiClient;
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void loadHistoricalRatesIfEmpty() {
        if (exchangeRateRepository.existsBy()) {
            log.info("Database already has exchange rate data; skipping bulk load");
            return;
        }
        log.info("Database is empty; starting bulk load of all historical exchange rates");
        List<ExchangeRate> rates = apiClient.fetchAllRates();
        persist(rates);
        log.info("Bulk load complete: {} rates persisted", rates.size());
    }

    @Scheduled(cron = "${exchange-rate.scheduler.cron}")
    @Transactional
    public void fetchAndStoreDailyRates() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        log.info("Fetching daily exchange rates for {}", yesterday);
        List<ExchangeRate> rates = apiClient.fetchRatesForDate(yesterday);
        persist(rates);
        log.info("Daily sync complete: {} rates persisted for {}", rates.size(), yesterday);
    }

    private void persist(List<ExchangeRate> rates) {
        for (ExchangeRate rate : rates) {
            String code = rate.currency().code();
            CurrencyEntity currency = currencyRepository.findByCode(code)
                    .orElseGet(() -> currencyRepository.save(new CurrencyEntity(code)));
            if (!exchangeRateRepository.existsByCurrencyCodeAndDate(code, rate.date())) {
                exchangeRateRepository.save(new ExchangeRateEntity(currency, rate.date(), rate.rate()));
            }
        }
    }
}
