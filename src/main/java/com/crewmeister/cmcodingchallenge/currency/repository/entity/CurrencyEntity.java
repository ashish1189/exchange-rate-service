package com.crewmeister.cmcodingchallenge.currency.repository.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "currencies")
public class CurrencyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 3)
    private String code;

    protected CurrencyEntity() {}

    public CurrencyEntity(String code) {
        this.code = code;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
}
