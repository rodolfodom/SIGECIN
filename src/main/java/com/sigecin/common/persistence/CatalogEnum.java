package com.sigecin.common.persistence;

/**
 * Enum que representa un catálogo fijo de la BD (role, service_status, ...).
 * Cada constante conoce el id numérico de su fila en el catálogo.
 */
public interface CatalogEnum {

    int getId();
}
