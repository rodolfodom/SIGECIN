package com.sigecin.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * TEMPORAL: destino del inicio de sesión del dueño mientras se construye el dashboard
 * (fase 6). Al reemplazarlo se elimina esta clase junto con templates/placeholder.html.
 */
@Controller
public class PlaceholderController {

    @GetMapping("/business/dashboard")
    public String dashboard() {
        return "placeholder";
    }
}
