package com.sigecin.serviceoffering.converter;

import com.sigecin.common.converter.CatalogEnumConverter;
import com.sigecin.serviceoffering.enums.ServiceStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ServiceStatusConverter extends CatalogEnumConverter<ServiceStatus> {

    public ServiceStatusConverter() {
        super(ServiceStatus.class);
    }
}
