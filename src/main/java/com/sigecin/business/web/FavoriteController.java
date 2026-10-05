package com.sigecin.business.web;

import com.sigecin.auth.security.AuthenticatedUser;
import com.sigecin.business.dto.FavoriteEntry;
import com.sigecin.business.service.FavoriteService;
import com.sigecin.common.web.LocalRedirects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Favoritos del cliente. Marcar y quitar se hacen desde el detalle del negocio o desde la
 * lista; el formulario envía en {@code redirect} la página a la que se regresa.
 */
@Controller
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private static final String LIST = "/favorites";

    private final FavoriteService favorites;

    @GetMapping
    public String list(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        List<FavoriteEntry> entries = favorites.list(user.id());
        model.addAttribute("favorites", entries);
        model.addAttribute("hasUnavailable", entries.stream().anyMatch(entry -> !entry.available()));
        return "client/favorites";
    }

    @PostMapping("/{businessId}")
    public String add(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long businessId,
                      @RequestParam(required = false) String redirect, RedirectAttributes flash) {
        favorites.add(user.id(), businessId);
        flash.addFlashAttribute("successKey", "favorite.added");
        return "redirect:" + LocalRedirects.orDefault(redirect, LIST);
    }

    @PostMapping("/{businessId}/remove")
    public String remove(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long businessId,
                         @RequestParam(required = false) String redirect, RedirectAttributes flash) {
        favorites.remove(user.id(), businessId);
        flash.addFlashAttribute("successKey", "favorite.removed");
        return "redirect:" + LocalRedirects.orDefault(redirect, LIST);
    }
}
