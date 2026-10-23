package com.caloriecounter.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Calorie forecast for the coming days, starting tomorrow.
 * Model: least-squares linear trend over the logged days of the last 28 days,
 * plus a damped day-of-week adjustment (e.g. weekends usually run higher).
 */
public record ForecastResponse(
        boolean available,
        String message,
        String method,
        int basedOnDays,
        int goal,
        /** Trend in kcal per day (positive = intake rising). */
        double slopePerDay,
        /** How much of the day-to-day variation the trend + weekday model explains (0–1). */
        double rSquared,
        Integer averagePredicted,
        int daysOverGoal,
        List<Point> points
) {
    public record Point(LocalDate date, int predicted, int low, int high) {
    }
}
