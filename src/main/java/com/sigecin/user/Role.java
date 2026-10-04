package com.sigecin.user;

import com.sigecin.common.persistence.CatalogEnum;

/** Catálogo role: CLIENT (cliente) y BUSINESS (dueño de negocio). */
public enum Role implements CatalogEnum {
    CLIENT(1),
    BUSINESS(2);

    private final int id;

    Role(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }
}
