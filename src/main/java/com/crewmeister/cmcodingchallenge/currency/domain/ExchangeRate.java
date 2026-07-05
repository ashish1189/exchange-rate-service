package com.crewmeister.cmcodingchallenge.currency.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "exchange_rates",
    uniqueConstraints = @UniqueConstraint(columnNames = {"currency", "date"})
)
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal rate;

    protected ExchangeRate() {}

    public ExchangeRate(String currency, LocalDate date, BigDecimal rate) {
        this.currency = currency;
        this.date = date;
        this.rate = rate;
    }

    public Long getId() { return id; }
    public String getCurrency() { return currency; }
    public LocalDate getDate() { return date; }
    public BigDecimal getRate() { return rate; }
}
