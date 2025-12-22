package com.carter.dto;

/**
 * DTO for search result entries.
 * Replaces the untyped Map&lt;String, Object&gt;.
 *
 * @author Carter
 * @since 1.0.0
 */
public record SearchResultDto(
        String employeeName,
        String professionalSummary,
        double similarity
) {}

