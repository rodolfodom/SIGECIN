package com.sigecin.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de POST /api/auth/login. */
public record LoginRequest(@NotBlank String email, @NotBlank String password) {
}
