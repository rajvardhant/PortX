package com.portx.routesystem.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * VehicleTypeConverter — Auto-applied JPA Attribute Converter.
 * Safely converts database strings to VehicleType enum attributes.
 * Intercepts any legacy 'BIKE' database records and maps them to 'VAN'
 * without throwing IllegalArgumentException 500 errors!
 */
@Converter(autoApply = true)
public class VehicleTypeConverter implements AttributeConverter<VehicleType, String> {

    @Override
    public String convertToDatabaseColumn(VehicleType attribute) {
        if (attribute == null) return "VAN";
        return attribute.name();
    }

    @Override
    public VehicleType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty() || "BIKE".equalsIgnoreCase(dbData.trim())) {
            return VehicleType.VAN;
        }
        try {
            return VehicleType.valueOf(dbData.trim().toUpperCase());
        } catch (Exception e) {
            return VehicleType.VAN;
        }
    }
}
