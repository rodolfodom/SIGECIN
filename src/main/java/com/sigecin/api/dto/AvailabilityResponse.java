package com.sigecin.api.dto;

import com.sigecin.appointment.dto.Availability;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Horas de inicio libres de un servicio en una fecha (vacío si el negocio no abre o está lleno). */
public record AvailabilityResponse(Long businessId, ServiceResponse service, LocalDate date, boolean open,
                                   List<LocalTime> slots) {

    public static AvailabilityResponse of(Availability availability) {
        return new AvailabilityResponse(availability.business().getId(), ServiceResponse.of(availability.service()),
                availability.date(), availability.day().open(), availability.slots());
    }
}
