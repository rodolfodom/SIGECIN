package com.sigecin.appointment.dto;

import com.sigecin.business.dto.DaySchedule;
import com.sigecin.business.entity.Business;
import com.sigecin.serviceoffering.entity.ServiceOffering;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Horas de inicio libres de un servicio en una fecha. {@code day} es el horario del negocio
 * ese día (cerrado si no abre); {@code slots} queda vacío si no hay ninguna hora libre.
 */
public record Availability(Business business, ServiceOffering service, LocalDate date,
                           DaySchedule day, List<LocalTime> slots) {
}
