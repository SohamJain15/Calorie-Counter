package com.caloriecounter.dto;

import com.caloriecounter.model.MealType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Body of POST/PUT /api/log/entries.
 * <p>
 * Either pass {@code foodId} (nutrition is taken from the food database and multiplied by quantity),
 * or leave it out and pass {@code name} + {@code calories} (and optionally macros) for ONE serving.
 * <pre>
 * { "date": "2026-10-23", "mealType": "breakfast", "time": "08:30", "foodId": 7, "quantity": 1.5 }
 * { "mealType": "snack", "name": "Masala chai", "servingSize": "1 cup", "calories": 90, "quantity": 2 }
 * </pre>
 */
public record LogEntryRequest(
        LocalDate date,

        @NotNull(message = "mealType is required (breakfast, lunch, dinner, snack)")
        MealType mealType,

        LocalTime time,

        Long foodId,

        @NotNull(message = "quantity is required")
        @DecimalMin(value = "0.1", message = "quantity must be at least 0.1")
        @DecimalMax(value = "50", message = "quantity must be at most 50")
        Double quantity,

        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @Size(max = 50, message = "servingSize must be at most 50 characters")
        String servingSize,

        @Min(value = 0, message = "calories cannot be negative")
        @Max(value = 10000, message = "calories looks too large")
        Integer calories,

        @DecimalMin(value = "0", message = "protein cannot be negative")
        @DecimalMax(value = "1000", message = "protein looks too large")
        Double protein,

        @DecimalMin(value = "0", message = "carbs cannot be negative")
        @DecimalMax(value = "1000", message = "carbs looks too large")
        Double carbs,

        @DecimalMin(value = "0", message = "fat cannot be negative")
        @DecimalMax(value = "1000", message = "fat looks too large")
        Double fat
) {
}
