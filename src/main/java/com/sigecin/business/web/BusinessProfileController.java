package com.sigecin.business.web;

import com.sigecin.auth.security.AuthenticatedUser;
import com.sigecin.business.dto.DaySchedule;
import com.sigecin.business.entity.Business;
import com.sigecin.business.exception.BusinessAlreadyExistsException;
import com.sigecin.business.service.BusinessService;
import com.sigecin.business.web.form.BusinessForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Alta, consulta, modificación, baja y reactivación del negocio del dueño. */
@Controller
@RequestMapping("/business")
@RequiredArgsConstructor
public class BusinessProfileController {

    private static final String FORM_VIEW = "business/profile-form";

    private final BusinessService businessService;

    // Recorta espacios y convierte los campos vacíos en null (teléfono y dirección son opcionales)
    @InitBinder("businessForm")
    void trimStrings(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping("/setup")
    public String setupForm(Model model) {
        return form(model, new BusinessForm(), true);
    }

    @PostMapping("/setup")
    public String setup(@AuthenticationPrincipal AuthenticatedUser user,
                        @Valid @ModelAttribute BusinessForm businessForm, BindingResult result,
                        Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return form(model, businessForm, true);
        }
        try {
            businessService.create(user.id(), businessForm.toData());
        } catch (BusinessAlreadyExistsException e) {
            redirect.addFlashAttribute("errorKey", e.getMessageKey());
            return "redirect:/business/profile";
        }
        redirect.addFlashAttribute("successKey", "business.setup.success");
        return "redirect:/business/profile";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        Business business = businessService.getByOwner(user.id());
        model.addAttribute("business", business);
        model.addAttribute("visibility", businessService.visibility(business));
        model.addAttribute("week", DaySchedule.weekOf(business));
        return "business/profile";
    }

    @GetMapping("/profile/edit")
    public String editForm(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        return form(model, BusinessForm.from(businessService.getByOwner(user.id())), false);
    }

    @PostMapping("/profile/edit")
    public String edit(@AuthenticationPrincipal AuthenticatedUser user,
                       @Valid @ModelAttribute BusinessForm businessForm, BindingResult result,
                       Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return form(model, businessForm, false);
        }
        businessService.update(user.id(), businessForm.toData());
        redirect.addFlashAttribute("successKey", "business.profile.updated");
        return "redirect:/business/profile";
    }

    @GetMapping("/profile/deactivate")
    public String confirmDeactivation(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        Business business = businessService.getByOwner(user.id());
        if (!business.isActive()) {
            return "redirect:/business/profile";
        }
        model.addAttribute("business", business);
        model.addAttribute("appointmentsToCancel", businessService.countAppointmentsCancelledOnDeactivation(user.id()));
        return "business/deactivate";
    }

    @PostMapping("/profile/deactivate")
    public String deactivate(@AuthenticationPrincipal AuthenticatedUser user, RedirectAttributes redirect) {
        int cancelled = businessService.deactivate(user.id());
        redirect.addFlashAttribute("successKey", "business.profile.deactivated");
        redirect.addFlashAttribute("successArg", String.valueOf(cancelled));
        return "redirect:/business/profile";
    }

    @PostMapping("/profile/activate")
    public String activate(@AuthenticationPrincipal AuthenticatedUser user, RedirectAttributes redirect) {
        businessService.activate(user.id());
        redirect.addFlashAttribute("successKey", "business.profile.activated");
        return "redirect:/business/profile";
    }

    private String form(Model model, BusinessForm form, boolean setup) {
        model.addAttribute("businessForm", form);
        model.addAttribute("categories", businessService.categories());
        model.addAttribute("setup", setup);
        model.addAttribute("phonePattern", BusinessForm.PHONE_PATTERN);
        return FORM_VIEW;
    }
}
