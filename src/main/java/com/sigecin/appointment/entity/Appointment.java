package com.sigecin.appointment.entity;

import com.sigecin.appointment.enums.AppointmentStatus;
import com.sigecin.serviceoffering.entity.ServiceOffering;
import com.sigecin.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cita de un cliente; el negocio se obtiene a través del servicio. */
@Entity
@Table(name = "appointment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Usuario con rol CLIENT
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering service;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    // Calculado: inicio + duración del servicio
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // Precio congelado al reservar; los reportes usan este valor
    @Column(name = "price_at_booking", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtBooking;

    @Column(name = "status_id", nullable = false)
    @Setter
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(name = "client_notes", length = 500)
    private String clientNotes;

    // Vacío mientras la cita no esté cancelada
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_reason_id")
    @Setter
    private CancellationReason cancelledReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    /** Crea la cita en PENDING, calcula el fin y copia el precio vigente del servicio. */
    public Appointment(User client, ServiceOffering service, LocalDateTime startTime, String clientNotes) {
        this.client = client;
        this.service = service;
        this.startTime = startTime;
        this.endTime = startTime.plusMinutes(service.getDurationMin());
        this.priceAtBooking = service.getPrice();
        this.clientNotes = clientNotes;
    }

    /** El cliente o el negocio aún pueden cancelarla: no está cancelada y no ha empezado. */
    public boolean isCancellable(LocalDateTime now) {
        return status != AppointmentStatus.CANCELLED && startTime.isAfter(now);
    }

    /** El negocio aún puede confirmarla: está pendiente y no ha empezado. */
    public boolean isConfirmable(LocalDateTime now) {
        return status == AppointmentStatus.PENDING && startTime.isAfter(now);
    }
}
