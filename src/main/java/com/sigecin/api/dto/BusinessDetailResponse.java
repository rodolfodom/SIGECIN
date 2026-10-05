package com.sigecin.api.dto;

import com.sigecin.business.dto.BusinessDetail;
import com.sigecin.business.dto.DaySchedule;

import java.util.List;

/** Detalle público de un negocio: datos, horario de la semana y servicios activos. */
public record BusinessDetailResponse(BusinessSummary business, List<DaySchedule> week,
                                     List<ServiceResponse> services) {

    public static BusinessDetailResponse of(BusinessDetail detail) {
        return new BusinessDetailResponse(BusinessSummary.of(detail.business()), detail.week(),
                detail.services().stream().map(ServiceResponse::of).toList());
    }
}
