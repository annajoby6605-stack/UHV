package com.fillthepockets.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReportStatusConverter implements AttributeConverter<ReportStatus, String> {

    @Override
    public String convertToDatabaseColumn(ReportStatus attribute) {
        if (attribute == null) {
            return null;
        }

        return attribute.name();
    }

    @Override
    public ReportStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        return ReportStatus.fromLegacyValue(dbData);
    }
}
