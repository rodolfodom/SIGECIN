package com.sigecin.appointment.enums;

import com.sigecin.common.enums.CatalogEnum;

/**
 * Catálogo appointment_status. Transiciones permitidas:
 * PENDING → CONFIRMED, PENDING → CANCELLED y CONFIRMED → CANCELLED.
 * CANCELLED es un estado final.
 */
public enum AppointmentStatus implements CatalogEnum {
    PENDING(1),
    CONFIRMED(2),
    CANCELLED(3);

    private final int id;

    AppointmentStatus(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    public boolean canTransitionTo(AppointmentStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == CANCELLED;
            case CANCELLED -> false;
        };
    }
}
