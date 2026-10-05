package com.sigecin.appointment.web.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Formulario de la página de disponibilidad. {@code businessId} y {@code date} solo sirven
 * para regresar a esa página si la reserva se rechaza: el negocio de la cita sale del servicio.
 */
@Getter
@Setter
public class BookingForm {

    private Long businessId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @NotNull
    private Long serviceId;

    // Valor del botón de la hora elegida, p. ej. 2026-10-12T10:30
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @Size(max = 500)
    private String clientNotes;
}
