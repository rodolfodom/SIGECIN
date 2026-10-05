package com.sigecin.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Cuerpo de POST /api/client/appointments; {@code startTime} con formato 2026-10-12T10:30:00. */
public record BookingRequest(@NotNull Long serviceId, @NotNull LocalDateTime startTime,
                             @Size(max = 500) String clientNotes) {
}
