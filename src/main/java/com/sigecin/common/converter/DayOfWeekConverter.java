package com.sigecin.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;

/**
 * business_schedule.day_of_week usa 1 = lunes … 7 = domingo (ISO 8601),
 * la misma numeración que {@link DayOfWeek#getValue()}. La columna es TINYINT.
 */
@Converter(autoApply = true)
public class DayOfWeekConverter implements AttributeConverter<DayOfWeek, Byte> {

    @Override
    public Byte convertToDatabaseColumn(DayOfWeek attribute) {
        return attribute == null ? null : (byte) attribute.getValue();
    }

    @Override
    public DayOfWeek convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : DayOfWeek.of(dbData);
    }
}
