package com.crewmeister.cmcodingchallenge.currency.client;

import com.crewmeister.cmcodingchallenge.currency.domain.ExchangeRate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Component
public class BundesbankApiClient {

    private final RestClient restClient;
    private final String dataflow;
    private final String key;

    public BundesbankApiClient(
            @Value("${bundesbank.api.base-url}") String baseUrl,
            @Value("${bundesbank.api.dataflow}") String dataflow,
            @Value("${bundesbank.api.key}") String key
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.dataflow = dataflow;
        this.key = key;
    }

    /**
     * Fetches all available EUR exchange rates (full history) from the Bundesbank API.
     */
    public List<ExchangeRate> fetchAllRates() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Fetches EUR exchange rates for a specific date from the Bundesbank API.
     */
    public List<ExchangeRate> fetchRatesForDate(LocalDate date) {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
