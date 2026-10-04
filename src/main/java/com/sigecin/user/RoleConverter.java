package com.sigecin.user;

import com.sigecin.common.persistence.CatalogEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleConverter extends CatalogEnumConverter<Role> {

    public RoleConverter() {
        super(Role.class);
    }
}
