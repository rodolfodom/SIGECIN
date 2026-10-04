package com.sigecin.appointment.web;

import com.sigecin.appointment.enums.AppointmentStatus;
import com.sigecin.appointment.enums.CancelledBy;
import com.sigecin.appointment.exception.AppointmentNotModifiableException;
import com.sigecin.appointment.service.AppointmentService;
import com.sigecin.appointment.web.view.CalendarView;
import com.sigecin.auth.security.AuthenticatedUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Citas del negocio: calendario, detalle, confirmar y cancelar. */
@Controller
@RequestMapping("/business/appointments")
public class BusinessAppointmentController {

    private final AppointmentService appointmentService;

    public BusinessAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /** Calendario de semana ({@code view=week}, por defecto) o de mes ({@code view=month}). */
    @GetMapping
    public String calendar(@AuthenticationPrincipal AuthenticatedUser user,
                           @RequestParam(required = false) String view,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam(required = false) AppointmentStatus status,
                           Model model) {
        CalendarView.Mode mode = CalendarView.Mode.fromParam(view);
        LocalDate today = LocalDate.now();
        LocalDate day = date == null ? today : date;
        var rows = appointmentService.calendar(user.id(),
                CalendarView.rangeStart(mode, day), CalendarView.rangeEnd(mode, day), status);
        model.addAttribute("calendar", CalendarView.of(mode, day, today, rows));
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("status", status);
        return "business/calendar";
    }

    @GetMapping("/{id}")
    public String detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id, Model model) {
        model.addAttribute("appointment", appointmentService.getForBusiness(user.id(), id));
        model.addAttribute("reasons", appointmentService.reasonsFor(CancelledBy.BUSINESS));
        model.addAttribute("now", LocalDateTime.now());
        return "business/appointment";
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                          RedirectAttributes redirect) {
        try {
            appointmentService.confirm(user.id(), id);
            redirect.addFlashAttribute("successKey", "appointment.confirmed");
        } catch (AppointmentNotModifiableException e) {
            redirect.addFlashAttribute("errorKey", e.getMessageKey());
        }
        return "redirect:/business/appointments/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                         @RequestParam(required = false) Integer reasonId, RedirectAttributes redirect) {
        try {
            appointmentService.cancelByBusiness(user.id(), id, reasonId);
            redirect.addFlashAttribute("successKey", "appointment.cancelled");
        } catch (AppointmentNotModifiableException e) {
            redirect.addFlashAttribute("errorKey", e.getMessageKey());
        }
        return "redirect:/business/appointments/" + id;
    }
}
