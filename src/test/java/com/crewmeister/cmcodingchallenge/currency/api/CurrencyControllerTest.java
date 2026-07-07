package com.crewmeister.cmcodingchallenge.currency.api;

import com.crewmeister.cmcodingchallenge.currency.api.dto.ConversionResponse;
import com.crewmeister.cmcodingchallenge.currency.api.dto.CurrencyResponse;
import com.crewmeister.cmcodingchallenge.currency.api.dto.ExchangeRateResponse;
import com.crewmeister.cmcodingchallenge.currency.exception.ExchangeRateNotFoundException;
import com.crewmeister.cmcodingchallenge.currency.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurrencyController.class)
class CurrencyControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ExchangeRateService exchangeRateService;

    private static final LocalDate DATE = LocalDate.of(2026, 1, 2);

    // --- GET /api/currencies ---

    @Test
    void should_return_200_with_list_of_currency_codes() throws Exception {
        when(exchangeRateService.getAvailableCurrencies())
                .thenReturn(List.of(
                        new CurrencyResponse("GBP", "Pound Sterling"),
                        new CurrencyResponse("USD", "US Dollar")));

        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currencyCode").value("GBP"))
                .andExpect(jsonPath("$[0].currencyName").value("Pound Sterling"))
                .andExpect(jsonPath("$[1].currencyCode").value("USD"))
                .andExpect(jsonPath("$[1].currencyName").value("US Dollar"));
    }

    @Test
    void should_return_200_with_empty_list_when_no_currencies_available() throws Exception {
        when(exchangeRateService.getAvailableCurrencies()).thenReturn(List.of());

        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- GET /api/exchange-rates ---

    @Test
    void should_return_200_with_all_exchange_rates() throws Exception {
        when(exchangeRateService.getAllRates()).thenReturn(List.of(
                new ExchangeRateResponse("GBP", DATE, new BigDecimal("0.8400")),
                new ExchangeRateResponse("USD", DATE, new BigDecimal("1.0500"))
        ));

        mockMvc.perform(get("/api/exchange-rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currency").value("GBP"))
                .andExpect(jsonPath("$[0].date").value("2026-01-02"))
                .andExpect(jsonPath("$[1].currency").value("USD"));
    }

    @Test
    void should_return_200_with_empty_list_when_no_rates_available() throws Exception {
        when(exchangeRateService.getAllRates()).thenReturn(List.of());

        mockMvc.perform(get("/api/exchange-rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- GET /api/exchange-rates/{currency}/{date} ---

    @Test
    void should_return_200_with_rate_for_currency_on_date() throws Exception {
        when(exchangeRateService.getRateForCurrencyOnDate("USD", DATE))
                .thenReturn(new ExchangeRateResponse("USD", DATE, new BigDecimal("1.0500")));

        mockMvc.perform(get("/api/exchange-rates/USD/2026-01-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.date").value("2026-01-02"))
                .andExpect(jsonPath("$.rate").value(1.05));
    }

    @Test
    void should_return_404_when_no_rate_found_for_currency_on_date() throws Exception {
        when(exchangeRateService.getRateForCurrencyOnDate("USD", DATE))
                .thenThrow(new ExchangeRateNotFoundException("USD", DATE));

        mockMvc.perform(get("/api/exchange-rates/USD/2026-01-02"))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/exchange-rates/convert ---

    @Test
    void should_return_200_with_conversion_result() throws Exception {
        when(exchangeRateService.convertToEur("USD", new BigDecimal("100"), DATE))
                .thenReturn(new ConversionResponse(
                        "USD", new BigDecimal("100"), DATE, DATE,
                        new BigDecimal("2.000000"), new BigDecimal("50.000000")));

        mockMvc.perform(get("/api/exchange-rates/convert")
                        .param("currency", "USD")
                        .param("amount", "100")
                        .param("date", "2026-01-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.requestedDate").value("2026-01-02"))
                .andExpect(jsonPath("$.rateDate").value("2026-01-02"))
                .andExpect(jsonPath("$.convertedAmountInEur").value(50.0));
    }

    @Test
    void should_return_404_when_no_rate_available_for_conversion() throws Exception {
        when(exchangeRateService.convertToEur(eq("USD"), any(BigDecimal.class), eq(DATE)))
                .thenThrow(new ExchangeRateNotFoundException("USD", DATE));

        mockMvc.perform(get("/api/exchange-rates/convert")
                        .param("currency", "USD")
                        .param("amount", "100")
                        .param("date", "2026-01-02"))
                .andExpect(status().isNotFound());
    }

    // --- 400 Bad Request ---

    @Test
    void should_return_400_when_date_path_variable_is_not_a_valid_date() throws Exception {
        mockMvc.perform(get("/api/exchange-rates/USD/not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_required_convert_param_is_missing() throws Exception {
        mockMvc.perform(get("/api/exchange-rates/convert")
                        .param("currency", "USD")
                        .param("amount", "100"))
                // date param is missing
                .andExpect(status().isBadRequest());
    }
}
