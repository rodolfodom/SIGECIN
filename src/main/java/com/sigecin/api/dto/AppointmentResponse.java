package com.sigecin.api.dto;

import com.sigecin.appointment.entity.Appointment;
import com.sigecin.appointment.entity.CancellationReason;
import com.sigecin.appointment.enums.AppointmentStatus;
import com.sigecin.serviceoffering.entity.ServiceOffering;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cita del cliente. La cita debe tener cargados servicio, negocio y motivo de cancelación. */
public record AppointmentResponse(Long id, Long businessId, String businessName, Long serviceId, String serviceName,
                                  LocalDateTime startTime, LocalDateTime endTime, BigDecimal price,
                                  AppointmentStatus status, String clientNotes, String cancellationReason) {

    public static AppointmentResponse of(Appointment appointment) {
        ServiceOffering service = appointment.getService();
        CancellationReason reason = appointment.getCancelledReason();
        return new AppointmentResponse(appointment.getId(), service.getBusiness().getId(),
                service.getBusiness().getName(), service.getId(), service.getName(),
                appointment.getStartTime(), appointment.getEndTime(), appointment.getPriceAtBooking(),
                appointment.getStatus(), appointment.getClientNotes(), reason == null ? null : reason.getName());
    }
}
