package com.caloriecounter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Food database categories, matching the filter chips on the Food database page. */
public enum FoodCategory {
    INDIAN, FRUITS, VEGETABLES, GRAINS, DAIRY, MEAT, OTHER;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static FoodCategory fromJson(String value) {
        if (value == null || value.isBlank()) return null;
        return FoodCategory.valueOf(value.trim().toUpperCase());
    }
}
