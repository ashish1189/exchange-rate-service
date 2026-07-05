package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.client.BundesbankApiClient;
import com.crewmeister.cmcodingchallenge.currency.repository.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ExchangeRateIngestionService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateIngestionService.class);

    private final BundesbankApiClient apiClient;
    private final ExchangeRateRepository repository;

    public ExchangeRateIngestionService(BundesbankApiClient apiClient,
                                        ExchangeRateRepository repository) {
        this.apiClient = apiClient;
        this.repository = repository;
    }

    /**
     * On startup: bulk loads all historical rates only when the database is empty.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadHistoricalRatesIfEmpty() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Daily job: fetches yesterday's rates and persists them.
     * Cron configured in application.properties (exchange-rate.scheduler.cron).
     */
    @Scheduled(cron = "${exchange-rate.scheduler.cron}")
    public void fetchAndStoreDailyRates() {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
