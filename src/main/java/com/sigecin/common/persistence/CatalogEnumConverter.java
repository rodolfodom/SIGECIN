package com.sigecin.common.persistence;

import jakarta.persistence.AttributeConverter;

/**
 * Convierte un {@link CatalogEnum} a su id numérico y viceversa.
 * Se conservan las FKs a los catálogos en la BD, pero en Java se usan enums.
 * Usa Byte porque los ids de los catálogos son TINYINT UNSIGNED.
 */
public abstract class CatalogEnumConverter<E extends Enum<E> & CatalogEnum>
        implements AttributeConverter<E, Byte> {

    private final Class<E> type;

    protected CatalogEnumConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public Byte convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : (byte) attribute.getId();
    }

    @Override
    public E convertToEntityAttribute(Byte id) {
        return id == null ? null : fromId(type, id);
    }

    public static <E extends Enum<E> & CatalogEnum> E fromId(Class<E> type, int id) {
        for (E constant : type.getEnumConstants()) {
            if (constant.getId() == id) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Id " + id + " no existe en el catálogo " + type.getSimpleName());
    }
}
