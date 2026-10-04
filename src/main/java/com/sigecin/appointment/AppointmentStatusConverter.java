package com.sigecin.appointment;

import com.sigecin.common.persistence.CatalogEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AppointmentStatusConverter extends CatalogEnumConverter<AppointmentStatus> {

    public AppointmentStatusConverter() {
        super(AppointmentStatus.class);
    }
}
