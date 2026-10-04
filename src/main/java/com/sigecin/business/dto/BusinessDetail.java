package com.sigecin.business.dto;

import com.sigecin.business.entity.Business;
import com.sigecin.serviceoffering.entity.ServiceOffering;

import java.util.List;

/** Detalle público de un negocio visible: datos (con categoría), semana de horario y servicios activos. */
public record BusinessDetail(Business business, List<DaySchedule> week, List<ServiceOffering> services) {
}
