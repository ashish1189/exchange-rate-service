package com.crewmeister.cmcodingchallenge.currency.repository;

import com.crewmeister.cmcodingchallenge.currency.repository.entity.ExchangeRateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, Long> {

    @Query(value = "SELECT e FROM ExchangeRateEntity e ORDER BY e.currency.code ASC, e.date ASC",
           countQuery = "SELECT COUNT(e) FROM ExchangeRateEntity e")
    Page<ExchangeRateEntity> findAllOrderByCurrencyAndDate(Pageable pageable);

    /**
     * Returns the most recent rate for a currency on or before the given date.
     * Handles weekends and public holidays by falling back to the last business day.
     */
    @Query("""
            SELECT e FROM ExchangeRateEntity e
            WHERE e.currency.code = :code AND e.date <= :date
            ORDER BY e.date DESC
            LIMIT 1
            """)
    Optional<ExchangeRateEntity> findLatestOnOrBefore(@Param("code") String code,
                                                      @Param("date") LocalDate date);

    boolean existsBy();

    boolean existsByCurrencyCodeAndDate(String code, LocalDate date);
}
