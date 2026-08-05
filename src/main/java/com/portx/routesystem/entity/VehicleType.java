package com.portx.routesystem.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * VehicleType Enum - Defines commercial vehicle categories (TRUCK, VAN).
 * Includes robust fallback logic for legacy BIKE values.
 */
public enum VehicleType {
    TRUCK,
    VAN,
    BIKE;

    @JsonCreator
    public static VehicleType fromString(String key) {
        if (key == null) return VAN;
        String upper = key.trim().toUpperCase();
        if ("BIKE".equals(upper)) return VAN;
        try {
            return VehicleType.valueOf(upper);
        } catch (Exception e) {
            return VAN;
        }
    }
}
