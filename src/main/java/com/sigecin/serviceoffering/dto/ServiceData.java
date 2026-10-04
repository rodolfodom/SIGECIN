package com.sigecin.serviceoffering.dto;

import java.math.BigDecimal;

/** Datos editables de un servicio. */
public record ServiceData(String name, String description, Integer durationMin, BigDecimal price) {
}
