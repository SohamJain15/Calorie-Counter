package com.caloriecounter.dto;

import com.caloriecounter.model.MealType;

import java.time.LocalDate;
import java.util.List;

/** Meal ideas that fit what is left of today's calorie and protein budget. */
public record SuggestionResponse(
        LocalDate date,
        MealType mealType,
        int remainingCalories,
        double remainingProtein,
        int targetCalories,
        String message,
        List<Suggestion> suggestions
) {
    public record Suggestion(String title, List<Item> items, int calories, double protein,
                             double carbs, double fat, String reason) {
    }

    public record Item(Long foodId, String name, double quantity, String servingSize, int calories) {
    }
}
