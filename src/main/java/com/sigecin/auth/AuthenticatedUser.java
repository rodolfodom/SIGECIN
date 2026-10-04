package com.sigecin.auth;

import com.sigecin.user.Role;

/**
 * Usuario autenticado, construido a partir de los claims del JWT.
 * Es el principal de la petición: los controladores lo reciben con
 * {@code @AuthenticationPrincipal} y las plantillas lo leen con
 * {@code #authentication.principal}.
 */
public record AuthenticatedUser(Long id, String fullName, String email, Role role) {
}
