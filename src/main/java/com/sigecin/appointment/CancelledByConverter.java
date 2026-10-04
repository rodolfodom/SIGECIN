package com.sigecin.appointment;

import com.sigecin.common.persistence.CatalogEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CancelledByConverter extends CatalogEnumConverter<CancelledBy> {

    public CancelledByConverter() {
        super(CancelledBy.class);
    }
}
