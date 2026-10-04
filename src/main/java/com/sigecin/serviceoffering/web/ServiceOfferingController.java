package com.sigecin.serviceoffering.web;

import com.sigecin.auth.security.AuthenticatedUser;
import com.sigecin.serviceoffering.enums.ServiceStatus;
import com.sigecin.serviceoffering.service.ServiceOfferingService;
import com.sigecin.serviceoffering.web.form.ServiceForm;
import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Servicios del negocio: lista, alta, edición y activar/desactivar (no se borran). */
@Controller
@RequestMapping("/business/services")
public class ServiceOfferingController {

    private static final String FORM_VIEW = "business/service-form";

    private final ServiceOfferingService serviceOfferingService;

    public ServiceOfferingController(ServiceOfferingService serviceOfferingService) {
        this.serviceOfferingService = serviceOfferingService;
    }

    @InitBinder("serviceForm")
    void trimStrings(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String list(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        model.addAttribute("services", serviceOfferingService.listOwned(user.id()));
        return "business/services";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("serviceForm", new ServiceForm());
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@AuthenticationPrincipal AuthenticatedUser user,
                         @Valid @ModelAttribute ServiceForm serviceForm, BindingResult result,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return FORM_VIEW;
        }
        serviceOfferingService.create(user.id(), serviceForm.toData());
        redirect.addFlashAttribute("successKey", "service.created");
        return "redirect:/business/services";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id, Model model) {
        model.addAttribute("serviceForm", ServiceForm.from(serviceOfferingService.getOwned(user.id(), id)));
        model.addAttribute("serviceId", id);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                         @Valid @ModelAttribute ServiceForm serviceForm, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            // Verifica la pertenencia también al volver a mostrar el formulario
            serviceOfferingService.getOwned(user.id(), id);
            model.addAttribute("serviceId", id);
            return FORM_VIEW;
        }
        serviceOfferingService.update(user.id(), id, serviceForm.toData());
        redirect.addFlashAttribute("successKey", "service.updated");
        return "redirect:/business/services";
    }

    @PostMapping("/{id}/status")
    public String toggleStatus(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                               RedirectAttributes redirect) {
        ServiceStatus status = serviceOfferingService.toggleStatus(user.id(), id);
        redirect.addFlashAttribute("successKey",
                status == ServiceStatus.ACTIVE ? "service.activated" : "service.deactivated");
        return "redirect:/business/services";
    }
}
