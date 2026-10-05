package com.sigecin.api.dto;

import com.sigecin.business.entity.Business;

/** Negocio visible en la búsqueda pública. La categoría debe estar cargada. */
public record BusinessSummary(Long id, String name, String category, String description,
                              String phone, String address) {

    public static BusinessSummary of(Business business) {
        return new BusinessSummary(business.getId(), business.getName(), business.getCategory().getName(),
                business.getDescription(), business.getPhone(), business.getAddress());
    }
}
