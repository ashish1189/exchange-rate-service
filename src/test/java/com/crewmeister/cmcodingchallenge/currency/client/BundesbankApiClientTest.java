package com.crewmeister.cmcodingchallenge.currency.client;

import com.crewmeister.cmcodingchallenge.currency.domain.ExchangeRate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(BundesbankApiClient.class)
@TestPropertySource(properties = {
        "bundesbank.api.base-url=http://test-server",
        "bundesbank.api.dataflow=BBEX3",
        "bundesbank.api.key=D..EUR.BB.AC.000"
})
class BundesbankApiClientTest {

    private static final String DATA_PATH = "/data/BBEX3/D..EUR.BB.AC.000";

    private static final String SAMPLE_CSV =
                    """
                    DATAFLOW;BBK_STD_FREQ;BBK_STD_CURRENCY;BBK_ERX_PARTNER_CURRENCY;\
                    BBK_ERX_SERIES_TYPE;BBK_ERX_RATE_TYPE;BBK_ERX_SUFFIX;TIME_PERIOD;OBS_VALUE;BBK_DIFF
                    BBK:BBEX3(1.0);D;USD;EUR;BB;AC;000;2026-07-06;1.0426;1.2
                    BBK:BBEX3(1.0);D;GBP;EUR;BB;AC;000;2026-07-06;0.83098;0.1
                    """;

    @Autowired
    private BundesbankApiClient client;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void should_fetch_all_rates_without_date_filter() {
        server.expect(requestTo(containsString(DATA_PATH)))
                .andExpect(queryParam("detail", "dataonly"))
                .andExpect(queryParam("format", "sdmx_csv"))
                .andRespond(withSuccess(SAMPLE_CSV, MediaType.TEXT_PLAIN));

        List<ExchangeRate> rates = client.fetchAllRates();

        assertThat(rates).hasSize(2);
        assertThat(rates.get(0).currency().code()).isEqualTo("USD");
        assertThat(rates.get(1).currency().code()).isEqualTo("GBP");
        server.verify();
    }

    @Test
    void should_fetch_rates_for_date_with_start_and_end_period_params() {
        LocalDate date = LocalDate.of(2026, Month.JULY, 6);

        server.expect(requestTo(containsString(DATA_PATH)))
                .andExpect(queryParam("detail", "dataonly"))
                .andExpect(queryParam("format", "sdmx_csv"))
                .andExpect(queryParam("startPeriod", "2026-07-06"))
                .andExpect(queryParam("endPeriod", "2026-07-06"))
                .andRespond(withSuccess(SAMPLE_CSV, MediaType.TEXT_PLAIN));

        List<ExchangeRate> rates = client.fetchRatesForDate(date);

        assertThat(rates).hasSize(2);
        server.verify();
    }

    @Test
    void should_return_empty_list_when_api_returns_no_data_rows() {
        String headerOnly =
                "DATAFLOW;BBK_STD_FREQ;BBK_STD_CURRENCY;BBK_ERX_PARTNER_CURRENCY;" +
                "BBK_ERX_SERIES_TYPE;BBK_ERX_RATE_TYPE;BBK_ERX_SUFFIX;TIME_PERIOD;OBS_VALUE;BBK_DIFF\n";

        server.expect(requestTo(containsString(DATA_PATH)))
                .andExpect(queryParam("detail", "dataonly"))
                .andExpect(queryParam("format", "sdmx_csv"))
                .andRespond(withSuccess(headerOnly, MediaType.TEXT_PLAIN));

        List<ExchangeRate> rates = client.fetchAllRates();

        assertThat(rates).isEmpty();
        server.verify();
    }
}
