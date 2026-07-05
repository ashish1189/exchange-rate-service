package com.crewmeister.cmcodingchallenge.currency.repository;

import com.crewmeister.cmcodingchallenge.currency.domain.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    List<String> findDistinctCurrencyBy();

    List<ExchangeRate> findAllByOrderByCurrencyAscDateAsc();

    List<ExchangeRate> findByCurrencyOrderByDateAsc(String currency);

    /**
     * Returns the most recent rate for a currency on or before the given date.
     * Handles weekends and public holidays by falling back to the last business day.
     */
    @Query("""
        SELECT e FROM ExchangeRate e
        WHERE e.currency = :currency AND e.date <= :date
        ORDER BY e.date DESC
        LIMIT 1
        """)
    Optional<ExchangeRate> findLatestOnOrBefore(@Param("currency") String currency,
                                                @Param("date") LocalDate date);

    boolean existsBy();
}
