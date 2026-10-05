package com.sigecin.business.web;

import com.sigecin.auth.security.AuthenticatedUser;
import com.sigecin.business.service.BusinessService;
import com.sigecin.business.service.ScheduleService;
import com.sigecin.business.web.form.ScheduleForm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Horario semanal del negocio. */
@Controller
@RequestMapping("/business/schedule")
@RequiredArgsConstructor
public class BusinessScheduleController {

    private final BusinessService businessService;
    private final ScheduleService scheduleService;

    @GetMapping
    public String form(@AuthenticationPrincipal AuthenticatedUser user, Model model) {
        model.addAttribute("scheduleForm", ScheduleForm.from(businessService.getByOwner(user.id())));
        return "business/schedule";
    }

    @PostMapping
    public String save(@AuthenticationPrincipal AuthenticatedUser user,
                       @ModelAttribute ScheduleForm scheduleForm, BindingResult result,
                       RedirectAttributes redirect) {
        scheduleForm.validate(result);
        if (result.hasErrors()) {
            return "business/schedule";
        }
        scheduleService.saveWeek(user.id(), scheduleForm.toData());
        redirect.addFlashAttribute("successKey", "business.schedule.saved");
        return "redirect:/business/schedule";
    }
}
