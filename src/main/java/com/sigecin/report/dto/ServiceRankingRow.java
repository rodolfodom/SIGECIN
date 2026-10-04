package com.sigecin.report.dto;

/** Fila de v_service_ranking_monthly; {@code ranking} = 1 es el más popular (puede haber empates). */
public record ServiceRankingRow(Long serviceId, String serviceName, long bookings, int ranking) {
}
