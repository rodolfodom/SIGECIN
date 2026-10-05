package com.sigecin.api.web;

import com.sigecin.api.dto.AppointmentResponse;
import com.sigecin.api.dto.BookingRequest;
import com.sigecin.api.dto.CancelRequest;
import com.sigecin.api.dto.PageResponse;
import com.sigecin.api.dto.ReasonResponse;
import com.sigecin.appointment.entity.Appointment;
import com.sigecin.appointment.enums.AppointmentStatus;
import com.sigecin.appointment.enums.CancelledBy;
import com.sigecin.appointment.service.AppointmentService;
import com.sigecin.appointment.service.BookingService;
import com.sigecin.auth.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/** Citas del cliente autenticado con el JWT (rol CLIENT). */
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientApiController {

    private final BookingService bookingService;
    private final AppointmentService appointmentService;

    @GetMapping("/appointments")
    public PageResponse<AppointmentResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @RequestParam(required = false) AppointmentStatus status,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                  @RequestParam(defaultValue = "0") int page) {
        return PageResponse.of(appointmentService.listForClient(user.id(), status, from, to, page),
                AppointmentResponse::of);
    }

    @GetMapping("/appointments/{id}")
    public AppointmentResponse detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return AppointmentResponse.of(appointmentService.getForClient(user.id(), id));
    }

    /** Reserva; si se rechaza responde 422 con el motivo (horario ocupado, fuera de horario, etc.). */
    @PostMapping("/appointments")
    public ResponseEntity<AppointmentResponse> book(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @Valid @RequestBody BookingRequest request) {
        Appointment booked = bookingService.book(user.id(), request.serviceId(), request.startTime(),
                request.clientNotes());
        return ResponseEntity.created(URI.create("/api/client/appointments/" + booked.getId()))
                .body(detail(user, booked.getId()));
    }

    @PostMapping("/appointments/{id}/cancel")
    public AppointmentResponse cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                      @Valid @RequestBody CancelRequest request) {
        appointmentService.cancelByClient(user.id(), id, request.reasonId());
        return detail(user, id);
    }

    @GetMapping("/cancellation-reasons")
    public List<ReasonResponse> reasons() {
        return appointmentService.reasonsFor(CancelledBy.CLIENT).stream().map(ReasonResponse::of).toList();
    }
}
