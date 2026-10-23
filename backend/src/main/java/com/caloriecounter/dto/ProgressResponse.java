package com.caloriecounter.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Calorie history for the Progress page.
 * Averages only count days that have at least one food logged.
 */
public record ProgressResponse(
        int days,
        LocalDate from,
        LocalDate to,
        int goal,
        int proteinGoal,
        List<DayPoint> points,
        int loggedDays,
        Integer averageConsumed,
        Integer previousAverage,
        Double changePercent,
        int withinGoalDays,
        Integer proteinCompletion
) {
    public record DayPoint(LocalDate date, int consumed, int burned, double protein, boolean logged) {
    }
}
