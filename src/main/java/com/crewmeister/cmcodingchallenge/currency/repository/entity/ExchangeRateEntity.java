package com.crewmeister.cmcodingchallenge.currency.repository.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "exchange_rates",
    uniqueConstraints = @UniqueConstraint(columnNames = {"currency_id", "date"})
)
public class ExchangeRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "currency_id", nullable = false)
    private CurrencyEntity currency;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal rate;

    protected ExchangeRateEntity() {}

    public ExchangeRateEntity(CurrencyEntity currency, LocalDate date, BigDecimal rate) {
        this.currency = currency;
        this.date = date;
        this.rate = rate;
    }

    public Long getId() { return id; }
    public CurrencyEntity getCurrency() { return currency; }
    public LocalDate getDate() { return date; }
    public BigDecimal getRate() { return rate; }
}
