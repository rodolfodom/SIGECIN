package com.sigecin.appointment.enums;

import com.sigecin.common.enums.CatalogEnum;

/**
 * Catálogo cancelled_by: actor al que corresponde un motivo de cancelación.
 * SYSTEM solo lo asigna la aplicación y no aparece en formularios.
 */
public enum CancelledBy implements CatalogEnum {
    CLIENT(1),
    BUSINESS(2),
    SYSTEM(3);

    private final int id;

    CancelledBy(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }
}
