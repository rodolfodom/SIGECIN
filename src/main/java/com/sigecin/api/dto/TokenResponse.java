package com.sigecin.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Par de tokens emitido por la API. {@code expiresIn} y {@code refreshExpiresIn} van en segundos.
 * {@code refreshToken} se omite cuando la renovación se aceptó dentro de la ventana de gracia
 * (otra petición simultánea ya recibió el token rotado).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(String tokenType, String accessToken, long expiresIn,
                            String refreshToken, Long refreshExpiresIn) {
}
