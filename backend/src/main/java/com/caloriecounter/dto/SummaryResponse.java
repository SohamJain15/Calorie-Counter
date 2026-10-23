package com.caloriecounter.dto;

import com.caloriecounter.model.MealType;

import java.time.LocalDate;
import java.util.List;

/**
 * Daily totals for the dashboard.
 * remaining = goal - consumed + burned; status is "Surplus" when remaining &lt; 0, otherwise "Deficit".
 */
public record SummaryResponse(
        LocalDate date,
        int goal,
        int consumed,
        int burned,
        int remaining,
        String status,
        int percentConsumed,
        double protein,
        double carbs,
        double fat,
        int proteinGoal,
        int carbsGoal,
        int fatGoal,
        List<MealSummary> meals
) {
    public record MealSummary(MealType mealType, int calories, int items) {
    }
}
