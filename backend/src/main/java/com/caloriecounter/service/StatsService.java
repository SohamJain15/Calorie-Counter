package com.caloriecounter.service;

import com.caloriecounter.dto.ProgressResponse;
import com.caloriecounter.dto.SummaryResponse;
import com.caloriecounter.model.ExerciseEntry;
import com.caloriecounter.model.FoodLogEntry;
import com.caloriecounter.model.MealType;
import com.caloriecounter.model.UserProfile;
import com.caloriecounter.repository.ExerciseEntryRepository;
import com.caloriecounter.repository.FoodLogEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final FoodLogEntryRepository entries;
    private final ExerciseEntryRepository exercises;
    private final ProfileService profileService;

    public StatsService(FoodLogEntryRepository entries, ExerciseEntryRepository exercises,
                        ProfileService profileService) {
        this.entries = entries;
        this.exercises = exercises;
        this.profileService = profileService;
    }

    /** Daily totals: calories, macros, per-meal breakdown and the Surplus/Deficit balance. */
    @Transactional
    public SummaryResponse summary(LocalDate date) {
        UserProfile profile = profileService.get();
        List<FoodLogEntry> food = entries.findByLogDateOrderByLoggedTimeAscIdAsc(date);
        List<ExerciseEntry> moves = exercises.findByLogDateOrderByLoggedTimeAscIdAsc(date);

        int consumed = food.stream().mapToInt(FoodLogEntry::getCalories).sum();
        int burned = moves.stream().mapToInt(ExerciseEntry::getCaloriesBurned).sum();
        int goal = profile.getCalorieGoal();
        int remaining = goal - consumed + burned;
        int percent = goal > 0 ? (int) Math.round(consumed * 100.0 / goal) : 0;

        List<SummaryResponse.MealSummary> meals = new ArrayList<>();
        for (MealType m : MealType.values()) {
            List<FoodLogEntry> inMeal = food.stream().filter(e -> e.getMealType() == m).toList();
            meals.add(new SummaryResponse.MealSummary(m,
                    inMeal.stream().mapToInt(FoodLogEntry::getCalories).sum(), inMeal.size()));
        }

        return new SummaryResponse(date, goal, consumed, burned, remaining,
                remaining < 0 ? "Surplus" : "Deficit", percent,
                LogService.round1(food.stream().mapToDouble(FoodLogEntry::getProtein).sum()),
                LogService.round1(food.stream().mapToDouble(FoodLogEntry::getCarbs).sum()),
                LogService.round1(food.stream().mapToDouble(FoodLogEntry::getFat).sum()),
                profile.getProteinGoal(), profile.getCarbsGoal(), profile.getFatGoal(), meals);
    }

    /** Last {@code days} days ending today, plus comparison with the period before. */
    @Transactional
    public ProgressResponse progress(int days) {
        UserProfile profile = profileService.get();
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days - 1L);

        Map<LocalDate, List<FoodLogEntry>> foodByDay = entries.findByLogDateBetween(from, to).stream()
                .collect(Collectors.groupingBy(FoodLogEntry::getLogDate));
        Map<LocalDate, Integer> burnedByDay = exercises.findByLogDateBetween(from, to).stream()
                .collect(Collectors.groupingBy(ExerciseEntry::getLogDate,
                        Collectors.summingInt(ExerciseEntry::getCaloriesBurned)));

        int goal = profile.getCalorieGoal();
        int proteinGoal = profile.getProteinGoal();
        List<ProgressResponse.DayPoint> points = new ArrayList<>();
        int loggedDays = 0;
        long consumedSum = 0;
        int withinGoal = 0;
        double proteinRatioSum = 0;

        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            List<FoodLogEntry> dayFood = foodByDay.getOrDefault(d, List.of());
            int consumed = dayFood.stream().mapToInt(FoodLogEntry::getCalories).sum();
            int burned = burnedByDay.getOrDefault(d, 0);
            double protein = LogService.round1(dayFood.stream().mapToDouble(FoodLogEntry::getProtein).sum());
            boolean logged = !dayFood.isEmpty();

            points.add(new ProgressResponse.DayPoint(d, consumed, burned, protein, logged));
            if (logged) {
                loggedDays++;
                consumedSum += consumed;
                if (consumed <= goal + burned) withinGoal++;
                if (proteinGoal > 0) proteinRatioSum += Math.min(protein / proteinGoal, 1.0);
            }
        }

        Integer average = loggedDays > 0 ? (int) Math.round((double) consumedSum / loggedDays) : null;
        Integer previous = previousAverage(from.minusDays(days), from.minusDays(1));
        Double change = (average != null && previous != null && previous > 0)
                ? LogService.round1((average - previous) * 100.0 / previous) : null;
        Integer proteinCompletion = (loggedDays > 0 && proteinGoal > 0)
                ? (int) Math.round(proteinRatioSum / loggedDays * 100) : null;

        return new ProgressResponse(days, from, to, goal, proteinGoal, points, loggedDays,
                average, previous, change, withinGoal, proteinCompletion);
    }

    private Integer previousAverage(LocalDate from, LocalDate to) {
        Map<LocalDate, Integer> perDay = entries.findByLogDateBetween(from, to).stream()
                .collect(Collectors.groupingBy(FoodLogEntry::getLogDate,
                        Collectors.summingInt(FoodLogEntry::getCalories)));
        if (perDay.isEmpty()) return null;
        return (int) Math.round(perDay.values().stream().mapToInt(Integer::intValue).average().orElse(0));
    }
}
