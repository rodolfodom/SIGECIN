package com.sigecin.business.dto;

/** Datos editables del negocio (los llena el formulario de alta y el de edición). */
public record BusinessData(Integer categoryId, String name, String description, String phone, String address) {
}
