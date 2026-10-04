package com.sigecin.auth;

import com.sigecin.auth.form.LoginForm;
import com.sigecin.auth.form.RegisterForm;
import com.sigecin.user.Role;
import com.sigecin.user.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Registro, inicio de sesión y la redirección inicial según el rol. */
@Controller
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final AuthCookies cookies;

    public AuthController(AuthService authService, JwtService jwtService, AuthCookies cookies) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.cookies = cookies;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/businesses";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterForm registerForm, BindingResult result,
                           RedirectAttributes redirect) {
        if (!result.hasFieldErrors("confirmPassword") && !registerForm.passwordsMatch()) {
            result.rejectValue("confirmPassword", "auth.register.password-mismatch");
        }
        if (result.hasErrors()) {
            return "auth/register";
        }
        try {
            authService.register(registerForm);
        } catch (EmailAlreadyRegisteredException e) {
            result.rejectValue("email", e.getMessageKey());
            return "auth/register";
        }
        redirect.addFlashAttribute("successKey", "auth.register.success");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String redirect, Model model) {
        LoginForm form = new LoginForm();
        form.setRedirect(redirect);
        model.addAttribute("loginForm", form);
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginForm loginForm, BindingResult result,
                        HttpServletResponse response, Model model) {
        if (result.hasErrors()) {
            return "auth/login";
        }
        User user;
        try {
            user = authService.authenticate(loginForm.getEmail(), loginForm.getPassword());
        } catch (BadCredentialsException e) {
            model.addAttribute("errorKey", "auth.login.bad-credentials");
            return "auth/login";
        } catch (DisabledException e) {
            model.addAttribute("errorKey", "auth.login.disabled");
            return "auth/login";
        }
        cookies.write(response, jwtService.issueToken(user));
        return "redirect:" + targetAfterLogin(loginForm.getRedirect(), user.getRole());
    }

    /** Vuelve a la página original solo si es una ruta local (evita redirecciones abiertas). */
    static String targetAfterLogin(String redirect, Role role) {
        boolean local = redirect != null && redirect.startsWith("/")
                && !redirect.startsWith("//") && !redirect.contains("\\");
        if (local) {
            return redirect;
        }
        return role == Role.BUSINESS ? "/business/dashboard" : "/businesses";
    }
}
