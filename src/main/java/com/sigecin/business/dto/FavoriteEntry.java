package com.sigecin.business.dto;

import com.sigecin.business.entity.Business;

/** Un favorito del cliente; {@code available} es falso si el negocio dejó de ser visible. */
public record FavoriteEntry(Business business, boolean available) {
}
