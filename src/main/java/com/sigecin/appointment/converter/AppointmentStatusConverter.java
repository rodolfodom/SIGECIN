package com.sigecin.appointment.converter;

import com.sigecin.appointment.enums.AppointmentStatus;
import com.sigecin.common.converter.CatalogEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AppointmentStatusConverter extends CatalogEnumConverter<AppointmentStatus> {

    public AppointmentStatusConverter() {
        super(AppointmentStatus.class);
    }
}
