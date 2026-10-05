package com.sigecin.api.dto;

import com.sigecin.appointment.entity.CancellationReason;

/** Motivo de cancelación disponible para el actor que cancela. */
public record ReasonResponse(Integer id, String name) {

    public static ReasonResponse of(CancellationReason reason) {
        return new ReasonResponse(reason.getId(), reason.getName());
    }
}
