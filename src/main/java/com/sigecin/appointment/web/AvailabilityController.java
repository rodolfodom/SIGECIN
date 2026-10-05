package com.sigecin.appointment.web;

import com.sigecin.appointment.service.AvailabilityService;
import com.sigecin.appointment.web.form.BookingForm;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Disponibilidad pública de un servicio. La página es también el formulario de reserva;
 * la sesión de cliente se pide al confirmar (el formulario envía POST /appointments).
 */
@Controller
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/businesses/{businessId}/availability")
    public String availability(@PathVariable Long businessId, @RequestParam Long serviceId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               Model model) {
        LocalDate today = LocalDate.now();
        // Sin fecha o con una fecha pasada se muestra hoy
        LocalDate day = date == null || date.isBefore(today) ? today : date;
        model.addAttribute("availability", availabilityService.forDate(businessId, serviceId, day));
        model.addAttribute("today", today);

        BookingForm form = new BookingForm();
        form.setBusinessId(businessId);
        form.setServiceId(serviceId);
        form.setDate(day);
        // Notas que el cliente ya había escrito, si la reserva anterior se rechazó
        form.setClientNotes((String) model.getAttribute("clientNotes"));
        model.addAttribute("bookingForm", form);
        return "client/availability";
    }
}
