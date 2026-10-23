package com.caloriecounter.dto;

import jakarta.validation.constraints.*;

/** Body of PUT /api/profile. */
public record ProfileRequest(
        @Size(max = 60, message = "name must be at most 60 characters")
        String name,

        @Min(value = 10, message = "age must be at least 10")
        @Max(value = 120, message = "age must be at most 120")
        Integer age,

        @Pattern(regexp = "male|female", message = "sex must be male or female")
        String sex,

        @DecimalMin(value = "50", message = "height must be at least 50 cm")
        @DecimalMax(value = "260", message = "height must be at most 260 cm")
        Double heightCm,

        @DecimalMin(value = "20", message = "weight must be at least 20 kg")
        @DecimalMax(value = "400", message = "weight must be at most 400 kg")
        Double weightKg,

        @Pattern(regexp = "sedentary|light|moderate|active|very_active",
                message = "activityLevel must be sedentary, light, moderate, active or very_active")
        String activityLevel,

        @NotNull(message = "calorieGoal is required")
        @Min(value = 800, message = "calorieGoal must be at least 800")
        @Max(value = 10000, message = "calorieGoal must be at most 10000")
        Integer calorieGoal,

        @NotNull(message = "proteinGoal is required")
        @Min(value = 0, message = "proteinGoal cannot be negative")
        @Max(value = 1000, message = "proteinGoal looks too large")
        Integer proteinGoal,

        @NotNull(message = "carbsGoal is required")
        @Min(value = 0, message = "carbsGoal cannot be negative")
        @Max(value = 2000, message = "carbsGoal looks too large")
        Integer carbsGoal,

        @NotNull(message = "fatGoal is required")
        @Min(value = 0, message = "fatGoal cannot be negative")
        @Max(value = 1000, message = "fatGoal looks too large")
        Integer fatGoal
) {
}
