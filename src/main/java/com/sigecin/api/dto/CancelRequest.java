package com.sigecin.api.dto;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de las cancelaciones: id del motivo (ver GET .../cancellation-reasons). */
public record CancelRequest(@NotNull Integer reasonId) {
}
