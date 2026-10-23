package com.caloriecounter.dto;

import com.caloriecounter.model.ExerciseEntry;
import com.caloriecounter.model.FoodLogEntry;

import java.time.LocalDate;
import java.util.List;

/** Everything logged on one day: foods (sorted by time) and exercise. */
public record DayLogResponse(LocalDate date, List<FoodLogEntry> entries, List<ExerciseEntry> exercises) {
}
