package com.sigecin.appointment.web;

import com.sigecin.appointment.entity.Appointment;
import com.sigecin.appointment.enums.AppointmentStatus;
import com.sigecin.appointment.enums.CancelledBy;
import com.sigecin.appointment.exception.AppointmentNotModifiableException;
import com.sigecin.appointment.exception.BookingRejectedException;
import com.sigecin.appointment.service.AppointmentService;
import com.sigecin.appointment.service.BookingService;
import com.sigecin.appointment.web.form.BookingForm;
import com.sigecin.auth.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Citas del cliente: reservar, listar, consultar y cancelar. */
@Controller
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class ClientAppointmentController {

    private final BookingService bookingService;
    private final AppointmentService appointmentService;

    @InitBinder("bookingForm")
    void trimStrings(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String list(@AuthenticationPrincipal AuthenticatedUser user,
                       @RequestParam(required = false) AppointmentStatus status,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("appointments", appointmentService.listForClient(user.id(), status, from, to, page));
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("status", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("now", LocalDateTime.now());
        return "client/appointments";
    }

    /**
     * Reserva. Si se rechaza (horario ocupado, fuera de horario, fecha pasada, servicio no
     * disponible) se vuelve a la disponibilidad con el motivo y con las notas escritas.
     */
    @PostMapping
    public String book(@AuthenticationPrincipal AuthenticatedUser user,
                       @Valid @ModelAttribute BookingForm bookingForm, BindingResult result,
                       RedirectAttributes redirect) {
        if (result.hasErrors()) {
            String key = result.hasFieldErrors("startTime") ? "booking.error.no-slot" : "booking.error.invalid";
            return backToAvailability(bookingForm, key, redirect);
        }
        try {
            Appointment appointment = bookingService.book(user.id(), bookingForm.getServiceId(),
                    bookingForm.getStartTime(), bookingForm.getClientNotes());
            redirect.addFlashAttribute("successKey", "booking.success");
            return "redirect:/appointments/" + appointment.getId();
        } catch (BookingRejectedException e) {
            return backToAvailability(bookingForm, e.getMessageKey(), redirect);
        }
    }

    @GetMapping("/{id}")
    public String detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id, Model model) {
        model.addAttribute("appointment", appointmentService.getForClient(user.id(), id));
        model.addAttribute("reasons", appointmentService.reasonsFor(CancelledBy.CLIENT));
        model.addAttribute("now", LocalDateTime.now());
        return "client/appointment";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                         @RequestParam(required = false) Integer reasonId, RedirectAttributes redirect) {
        try {
            appointmentService.cancelByClient(user.id(), id, reasonId);
            redirect.addFlashAttribute("successKey", "appointment.cancelled");
        } catch (AppointmentNotModifiableException e) {
            redirect.addFlashAttribute("errorKey", e.getMessageKey());
        }
        return "redirect:/appointments/" + id;
    }

    private static String backToAvailability(BookingForm form, String errorKey, RedirectAttributes redirect) {
        redirect.addFlashAttribute("errorKey", errorKey);
        if (form.getClientNotes() != null) {
            redirect.addFlashAttribute("clientNotes", form.getClientNotes());
        }
        if (form.getBusinessId() == null || form.getServiceId() == null) {
            return "redirect:/businesses";
        }
        LocalDate date = form.getStartTime() != null ? form.getStartTime().toLocalDate() : form.getDate();
        return "redirect:" + UriComponentsBuilder.fromPath("/businesses/{id}/availability")
                .queryParam("serviceId", form.getServiceId())
                .queryParamIfPresent("date", java.util.Optional.ofNullable(date))
                .buildAndExpand(form.getBusinessId())
                .toUriString();
    }
}
