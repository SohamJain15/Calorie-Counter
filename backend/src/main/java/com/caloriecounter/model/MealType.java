package com.caloriecounter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Meal a food log entry belongs to. Serialized as lowercase: "breakfast", "lunch", "dinner", "snack". */
public enum MealType {
    BREAKFAST, LUNCH, DINNER, SNACK;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static MealType fromJson(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim().toUpperCase();
        if (v.equals("SNACKS")) v = "SNACK";
        return MealType.valueOf(v);
    }
}
