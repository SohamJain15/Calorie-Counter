package com.caloriecounter.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

/** Body of POST/PUT /api/log/exercises. */
public record ExerciseRequest(
        LocalDate date,

        LocalTime time,

        @NotBlank(message = "name is required, e.g. \"Evening run\"")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @Min(value = 1, message = "durationMinutes must be at least 1")
        @Max(value = 1440, message = "durationMinutes cannot be more than a day")
        Integer durationMinutes,

        @NotNull(message = "caloriesBurned is required")
        @Min(value = 0, message = "caloriesBurned cannot be negative")
        @Max(value = 5000, message = "caloriesBurned looks too large")
        Integer caloriesBurned
) {
}
