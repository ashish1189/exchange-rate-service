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
            RestClient.Builder restClientBuilder,
            @Value("${bundesbank.api.base-url}") String baseUrl,
            @Value("${bundesbank.api.dataflow}") String dataflow,
            @Value("${bundesbank.api.key}") String key
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.dataflow = dataflow;
        this.key = key;
    }

    public List<ExchangeRate> fetchAllRates() {
        String csv = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/data/{dataflow}/{key}")
                        .queryParam("detail", "dataonly")
                        .queryParam("format", "sdmx_csv")
                        .build(dataflow, key))
                .retrieve()
                .body(String.class);

        return BundesbankCsvParser.parse(csv);
    }

    public List<ExchangeRate> fetchRatesForDate(LocalDate date) {
        String dateStr = date.toString();
        String csv = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/data/{dataflow}/{key}")
                        .queryParam("detail", "dataonly")
                        .queryParam("format", "sdmx_csv")
                        .queryParam("startPeriod", dateStr)
                        .queryParam("endPeriod", dateStr)
                        .build(dataflow, key))
                .retrieve()
                .body(String.class);

        return BundesbankCsvParser.parse(csv);
    }
}
