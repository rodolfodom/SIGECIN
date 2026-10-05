package com.sigecin.auth.web.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginForm {

    @NotBlank
    private String email;

    @NotBlank
    private String password;

    // Página a la que se vuelve tras iniciar sesión (solo rutas locales)
    private String redirect;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}
