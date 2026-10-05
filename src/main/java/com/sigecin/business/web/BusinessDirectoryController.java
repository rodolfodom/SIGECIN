package com.sigecin.business.web;

import com.sigecin.auth.security.AuthenticatedUser;
import com.sigecin.business.service.BusinessDirectoryService;
import com.sigecin.business.service.FavoriteService;
import com.sigecin.user.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Búsqueda y detalle públicos de negocios (rutas sin sesión obligatoria). */
@Controller
@RequestMapping("/businesses")
@RequiredArgsConstructor
public class BusinessDirectoryController {

    private final BusinessDirectoryService directory;
    private final FavoriteService favorites;

    @GetMapping
    public String search(@RequestParam(required = false) String name,
                         @RequestParam(required = false) Integer categoryId,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "" + BusinessDirectoryService.DEFAULT_PAGE_SIZE) int size,
                         Model model) {
        model.addAttribute("results", directory.search(name, categoryId, page, size));
        model.addAttribute("categories", directory.categories());
        model.addAttribute("name", name == null ? "" : name.trim());
        model.addAttribute("categoryId", categoryId);
        return "client/businesses";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user, Model model) {
        model.addAttribute("detail", directory.getVisible(id));
        if (user != null && user.role() == Role.CLIENT) {
            model.addAttribute("favorite", favorites.isFavorite(user.id(), id));
        }
        return "client/business-detail";
    }
}
