package com.sigecin.auth.form;

import jakarta.validation.constraints.NotBlank;

public class LoginForm {

    @NotBlank
    private String email;

    @NotBlank
    private String password;

    // Página a la que se vuelve tras iniciar sesión (solo rutas locales)
    private String redirect;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }
}
