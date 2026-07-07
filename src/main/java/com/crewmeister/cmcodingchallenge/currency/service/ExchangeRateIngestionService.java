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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        persistBulk(rates);
        log.info("Bulk load complete: {} rates persisted", rates.size());
    }

    @Scheduled(cron = "${exchange-rate.scheduler.cron}")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "currencies", allEntries = true),
            @CacheEvict(value = "rates", allEntries = true)
    })
    public void fetchAndStoreDailyRates() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        log.info("Fetching daily exchange rates for {}", yesterday);
        List<ExchangeRate> rates = apiClient.fetchRatesForDate(yesterday);
        persist(rates);
        log.info("Daily sync complete: {} rates persisted for {}", rates.size(), yesterday);
    }

    // Used on startup when DB is empty: caches currency entities locally to avoid N+1 findByCode
    // calls, skips per-row existence checks (DB is guaranteed empty), and batch-saves all rates.
    private void persistBulk(List<ExchangeRate> rates) {
        Map<String, CurrencyEntity> currencyCache = new HashMap<>();
        List<ExchangeRateEntity> batch = new ArrayList<>(rates.size());

        for (ExchangeRate rate : rates) {
            String code = rate.currency().code();
            CurrencyEntity currency = currencyCache.computeIfAbsent(code,
                    c -> currencyRepository.findByCode(c)
                            .orElseGet(() -> currencyRepository.save(new CurrencyEntity(c))));
            batch.add(new ExchangeRateEntity(currency, rate.date(), rate.rate()));
        }

        exchangeRateRepository.saveAll(batch);
    }

    // Used by the daily scheduler: checks for duplicates since the DB already has data.
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
