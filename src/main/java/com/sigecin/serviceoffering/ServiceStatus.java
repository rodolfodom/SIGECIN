package com.sigecin.serviceoffering;

import com.sigecin.common.persistence.CatalogEnum;

/** Catálogo service_status. */
public enum ServiceStatus implements CatalogEnum {
    ACTIVE(1),
    INACTIVE(2);

    private final int id;

    ServiceStatus(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }
}
