package com.crewmeister.cmcodingchallenge.currency.repository.mapper;

import com.crewmeister.cmcodingchallenge.currency.domain.Currency;
import com.crewmeister.cmcodingchallenge.currency.domain.ExchangeRate;
import com.crewmeister.cmcodingchallenge.currency.repository.entity.ExchangeRateEntity;

public final class ExchangeRateMapper {

    private ExchangeRateMapper() {}

    public static ExchangeRate toDomain(ExchangeRateEntity entity) {
        return new ExchangeRate(
                new Currency(entity.getCurrency().getCode()),
                entity.getDate(),
                entity.getRate()
        );
    }
}
