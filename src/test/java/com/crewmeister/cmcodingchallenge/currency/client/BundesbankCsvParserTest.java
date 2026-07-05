package com.crewmeister.cmcodingchallenge.currency.client;

import com.crewmeister.cmcodingchallenge.currency.domain.ExchangeRate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BundesbankCsvParserTest {

    private static final String HEADER =
            "DATAFLOW;BBK_STD_FREQ;BBK_STD_CURRENCY;BBK_ERX_PARTNER_CURRENCY;" +
            "BBK_ERX_SERIES_TYPE;BBK_ERX_RATE_TYPE;BBK_ERX_SUFFIX;TIME_PERIOD;OBS_VALUE;BBK_DIFF";

    @Test
    void should_parse_valid_rows_into_exchange_rate_domain_objects() {
        String csv = HEADER + "\n" +
                "BBK:BBEX3(1.0);D;USD;EUR;BB;AC;000;2026-07-06;1.0426;1.2\n" +
                "BBK:BBEX3(1.0);D;GBP;EUR;BB;AC;000;2026-07-06;0.83098;0.1\n";

        List<ExchangeRate> rates = BundesbankCsvParser.parse(csv);

        assertThat(rates).hasSize(2);

        ExchangeRate usd = rates.getFirst();
        assertThat(usd.currency().code()).isEqualTo("USD");
        assertThat(usd.date()).isEqualTo(LocalDate.of(2026, Month.JULY, 6));
        assertThat(usd.rate()).isEqualByComparingTo(new BigDecimal("1.0426"));

        ExchangeRate gbp = rates.get(1);
        assertThat(gbp.currency().code()).isEqualTo("GBP");
        assertThat(gbp.rate()).isEqualByComparingTo(new BigDecimal("0.83098"));
    }

    @Test
    void should_skip_rows_where_obs_value_is_missing() {
        String csv = HEADER + "\n" +
                "BBK:BBEX3(1.0);D;USD;EUR;BB;AC;000;2026-07-04;.;\n" +
                "BBK:BBEX3(1.0);D;USD;EUR;BB;AC;000;2026-07-06;1.0426;1.2\n";

        List<ExchangeRate> rates = BundesbankCsvParser.parse(csv);

        assertThat(rates).hasSize(1);
        assertThat(rates.getFirst().date()).isEqualTo(LocalDate.of(2026, Month.JULY, 6));
    }

    @Test
    void should_return_empty_list_for_header_only_csv() {
        List<ExchangeRate> rates = BundesbankCsvParser.parse(HEADER + "\n");

        assertThat(rates).isEmpty();
    }

    @Test
    void should_return_empty_list_for_blank_input() {
        assertThat(BundesbankCsvParser.parse("")).isEmpty();
        assertThat(BundesbankCsvParser.parse("   ")).isEmpty();
    }

    @Test
    void should_parse_multiple_currencies_on_same_date() {
        String csv = HEADER + "\n" +
                "BBK:BBEX3(1.0);D;USD;EUR;BB;AC;000;2026-07-06;1.0426;1.2\n" +
                "BBK:BBEX3(1.0);D;JPY;EUR;BB;AC;000;2026-07-06;163.25;0.9\n" +
                "BBK:BBEX3(1.0);D;CHF;EUR;BB;AC;000;2026-07-06;0.9396;0.4\n";

        List<ExchangeRate> rates = BundesbankCsvParser.parse(csv);

        assertThat(rates).hasSize(3);
        assertThat(rates).extracting(r -> r.currency().code())
                .containsExactly("USD", "JPY", "CHF");
    }
}
