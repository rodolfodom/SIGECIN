package com.sigecin.appointment.entity;

import com.sigecin.appointment.enums.CancelledBy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Motivo de cancelación (en español, se muestra al usuario), asociado al actor que cancela. */
@Entity
@Table(name = "appointment_cancellation_reason")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CancellationReason {

    /** Motivo que se asigna a las citas futuras cuando el dueño da de baja su negocio. */
    public static final String BUSINESS_DEACTIVATED = "Cancelada por el dueño del negocio";

    /** Motivo de actor SYSTEM para las citas PENDING que llegaron a su hora sin confirmarse. */
    public static final String NOT_CONFIRMED_IN_TIME = "Cancelada automáticamente: no fue confirmada a tiempo";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "cancelled_by_id", nullable = false)
    private CancelledBy cancelledBy;
}
