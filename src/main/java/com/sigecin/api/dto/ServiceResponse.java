package com.sigecin.api.dto;

import com.sigecin.serviceoffering.entity.ServiceOffering;

import java.math.BigDecimal;

/** Servicio activo de un negocio. */
public record ServiceResponse(Long id, String name, String description, Integer durationMin, BigDecimal price) {

    public static ServiceResponse of(ServiceOffering service) {
        return new ServiceResponse(service.getId(), service.getName(), service.getDescription(),
                service.getDurationMin(), service.getPrice());
    }
}
