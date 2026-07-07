package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.api.dto.CurrencyResponse;
import com.crewmeister.cmcodingchallenge.currency.client.BundesbankApiClient;
import com.crewmeister.cmcodingchallenge.currency.repository.CurrencyRepository;
import com.crewmeister.cmcodingchallenge.currency.repository.entity.CurrencyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureTestDatabase
class CachingVerificationTest {

    @Autowired
    private ExchangeRateService exchangeRateService;

    @SpyBean
    private CurrencyRepository currencyRepository;

    @MockBean
    private BundesbankApiClient bundesbankApiClient;

    @Test
    void should_hit_repository_only_once_for_repeated_calls() {
        currencyRepository.save(new CurrencyEntity("USD"));

        List<CurrencyResponse> first = exchangeRateService.getAvailableCurrencies();
        List<CurrencyResponse> second = exchangeRateService.getAvailableCurrencies();
        List<CurrencyResponse> third = exchangeRateService.getAvailableCurrencies();

        assertThat(first).isEqualTo(second).isEqualTo(third);
        verify(currencyRepository, times(1)).findAllByOrderByCodeAsc();
    }
}
