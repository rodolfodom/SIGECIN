package com.sigecin.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de POST /api/auth/refresh y POST /api/auth/logout. */
public record RefreshRequest(@NotBlank String refreshToken) {
}
