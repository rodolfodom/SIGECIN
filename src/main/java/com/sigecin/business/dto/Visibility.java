package com.sigecin.business.dto;

/**
 * Qué le falta a un negocio para aparecer en la búsqueda de los clientes:
 * estar activo, tener al menos un servicio activo y al menos un día de horario activo.
 */
public record Visibility(boolean active, boolean hasActiveService, boolean hasActiveSchedule) {

    public boolean visible() {
        return active && hasActiveService && hasActiveSchedule;
    }
}
