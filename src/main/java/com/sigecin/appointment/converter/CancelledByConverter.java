package com.sigecin.appointment.converter;

import com.sigecin.appointment.enums.CancelledBy;
import com.sigecin.common.converter.CatalogEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CancelledByConverter extends CatalogEnumConverter<CancelledBy> {

    public CancelledByConverter() {
        super(CancelledBy.class);
    }
}
