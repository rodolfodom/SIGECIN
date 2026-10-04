package com.sigecin.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Motivo de cancelación (en español, se muestra al usuario), asociado al actor que cancela. */
@Entity
@Table(name = "appointment_cancellation_reason")
public class CancellationReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "cancelled_by_id", nullable = false)
    private CancelledBy cancelledBy;

    protected CancellationReason() {
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CancelledBy getCancelledBy() {
        return cancelledBy;
    }
}
