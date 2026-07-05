package com.crewmeister.cmcodingchallenge.currency.client;

import com.crewmeister.cmcodingchallenge.currency.domain.Currency;
import com.crewmeister.cmcodingchallenge.currency.domain.ExchangeRate;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

final class BundesbankCsvParser {

    private static final char DELIMITER = ';';
    private static final String COL_CURRENCY = "BBK_STD_CURRENCY";
    private static final String COL_DATE = "TIME_PERIOD";
    private static final String COL_RATE = "OBS_VALUE";
    private static final String MISSING_VALUE = ".";

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setDelimiter(DELIMITER)
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build();

    private BundesbankCsvParser() {}

    static List<ExchangeRate> parse(String csv) {
        if (csv == null || csv.isBlank()) return List.of();

        try (CSVParser parser = CSVParser.parse(csv, FORMAT)) {
            return parser.stream()
                    .filter(csvRecord -> isValidRate(csvRecord.get(COL_RATE)))
                    .map(csvRecord -> new ExchangeRate(
                            new Currency(csvRecord.get(COL_CURRENCY)),
                            LocalDate.parse(csvRecord.get(COL_DATE)),
                            new BigDecimal(csvRecord.get(COL_RATE))
                    ))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse Bundesbank CSV response", e);
        }
    }

    private static boolean isValidRate(String value) {
        return value != null && !value.isBlank() && !MISSING_VALUE.equals(value);
    }
}
