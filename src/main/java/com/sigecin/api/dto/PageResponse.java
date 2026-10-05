package com.sigecin.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Página de resultados con un formato JSON estable (Page de Spring no se serializa directamente). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
