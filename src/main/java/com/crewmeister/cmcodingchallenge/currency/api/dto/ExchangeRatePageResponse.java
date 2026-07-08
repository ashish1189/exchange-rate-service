package com.crewmeister.cmcodingchallenge.currency.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Custom pagination envelope for exchange rate responses.
 * <p>
 * Spring's Page<T> could be returned directly, but it serialises to a verbose JSON shape that
 * exposes Spring Data internals (e.g. "pageable", "sort" objects) which have no meaning to API
 * consumers. This record produces a clean, stable contract with only the fields a frontend needs
 * to implement pagination controls, decoupled from the underlying framework.
 */
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
