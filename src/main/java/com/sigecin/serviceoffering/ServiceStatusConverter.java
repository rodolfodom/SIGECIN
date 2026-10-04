package com.sigecin.serviceoffering;

import com.sigecin.common.persistence.CatalogEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ServiceStatusConverter extends CatalogEnumConverter<ServiceStatus> {

    public ServiceStatusConverter() {
        super(ServiceStatus.class);
    }
}
