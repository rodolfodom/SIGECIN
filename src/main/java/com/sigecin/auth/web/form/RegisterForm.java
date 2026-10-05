package com.sigecin.auth.web.form;

import com.sigecin.user.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Formulario de registro; la coincidencia de contraseñas se valida en el controlador. */
@Getter
@Setter
public class RegisterForm {

    @NotBlank
    @Size(max = 120)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 150)
    private String email;

    // bcrypt solo considera los primeros 72 bytes
    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    private String confirmPassword;

    @NotNull
    private Role role = Role.CLIENT;

    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}
