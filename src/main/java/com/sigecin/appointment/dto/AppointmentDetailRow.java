package com.sigecin.appointment.dto;

import com.sigecin.appointment.enums.AppointmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una fila de la vista v_appointment_detail: la cita con negocio, cliente, servicio,
 * estado y motivo de cancelación. Base del calendario (y del dashboard y el reporte).
 */
public record AppointmentDetailRow(
        Long appointmentId,
        Long businessId,
        String businessName,
        Long clientId,
        String clientName,
        Long serviceId,
        String serviceName,
        BigDecimal price,
        Integer durationMin,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentStatus status,
        String clientNotes,
        String cancellationReason) {
}
