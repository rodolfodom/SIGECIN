package com.sigecin.user.converter;

import com.sigecin.common.converter.CatalogEnumConverter;
import com.sigecin.user.enums.Role;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleConverter extends CatalogEnumConverter<Role> {

    public RoleConverter() {
        super(Role.class);
    }
}
