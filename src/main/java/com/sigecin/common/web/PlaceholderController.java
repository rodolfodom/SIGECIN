package com.sigecin.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * TEMPORAL: destinos de inicio de sesión mientras se construyen sus módulos.
 * /businesses se reemplaza en la fase 4 y /business/dashboard en la fase 6.
 */
@Controller
public class PlaceholderController {

    @GetMapping("/businesses")
    public String businesses() {
        return "placeholder";
    }

    @GetMapping("/business/dashboard")
    public String dashboard() {
        return "placeholder";
    }
}
