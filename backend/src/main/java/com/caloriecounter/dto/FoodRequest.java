package com.caloriecounter.dto;

import com.caloriecounter.model.FoodCategory;
import jakarta.validation.constraints.*;

/** Body of POST /api/foods — adds a custom food to the database. Values are per one serving. */
public record FoodRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotNull(message = "category is required")
        FoodCategory category,

        @NotBlank(message = "servingSize is required, e.g. \"1 cup\"")
        @Size(max = 50, message = "servingSize must be at most 50 characters")
        String servingSize,

        @NotNull(message = "calories is required")
        @Min(value = 0, message = "calories cannot be negative")
        @Max(value = 5000, message = "calories per serving looks too large")
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
