package com.g6.ff.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ChangeTypeConverter implements AttributeConverter<Stock.ChangeType, String> {

    @Override
    public String convertToDatabaseColumn(Stock.ChangeType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDbValue();
    }

    @Override
    public Stock.ChangeType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        for (Stock.ChangeType type : Stock.ChangeType.values()) {
            if (type.getDbValue().equals(dbData)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ChangeType: " + dbData);
    }
}
