package com.crewmeister.cmcodingchallenge.currency.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record ExchangeRatePageResponse<T>(
        List<T> content,
        int page,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> ExchangeRatePageResponse<T> from(Page<T> page) {
        return new ExchangeRatePageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
