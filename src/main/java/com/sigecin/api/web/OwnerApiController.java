package com.sigecin.api.web;

import com.sigecin.api.dto.CancelRequest;
import com.sigecin.api.dto.ReasonResponse;
import com.sigecin.appointment.dto.AppointmentDetailRow;
import com.sigecin.appointment.enums.AppointmentStatus;
import com.sigecin.appointment.enums.CancelledBy;
import com.sigecin.appointment.service.AppointmentService;
import com.sigecin.auth.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/** Citas del negocio del dueño autenticado con el JWT (rol BUSINESS). */
@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
public class OwnerApiController {

    // Un mes con margen: evita consultas sin límite
    private static final int MAX_RANGE_DAYS = 62;

    private final AppointmentService appointmentService;

    /** Citas que empiezan en [from, to); por defecto los próximos 7 días a partir de hoy. */
    @GetMapping("/appointments")
    public List<AppointmentDetailRow> list(@AuthenticationPrincipal AuthenticatedUser user,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                           @RequestParam(required = false) AppointmentStatus status) {
        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end = to != null ? to : start.plusDays(7);
        if (!end.isAfter(start) || end.isAfter(start.plusDays(MAX_RANGE_DAYS))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El rango debe cumplir from < to y no pasar de " + MAX_RANGE_DAYS + " días");
        }
        return appointmentService.calendar(user.id(), start, end, status);
    }

    @PostMapping("/appointments/{id}/confirm")
    public ResponseEntity<Void> confirm(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        appointmentService.confirm(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/appointments/{id}/cancel")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                       @Valid @RequestBody CancelRequest request) {
        appointmentService.cancelByBusiness(user.id(), id, request.reasonId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cancellation-reasons")
    public List<ReasonResponse> reasons() {
        return appointmentService.reasonsFor(CancelledBy.BUSINESS).stream().map(ReasonResponse::of).toList();
    }
}
