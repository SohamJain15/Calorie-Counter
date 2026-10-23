package com.caloriecounter.service;

import com.caloriecounter.dto.ForecastResponse;
import com.caloriecounter.model.FoodLogEntry;
import com.caloriecounter.repository.FoodLogEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Predicts daily calorie intake for the next days.
 *
 * <ol>
 *   <li>Take the logged days in the last 28 days (today is excluded because it is not finished).</li>
 *   <li>Fit a straight line y = a + b·x by least squares, where x is the day number and y the calories.</li>
 *   <li>Day-of-week pattern: average the residuals (actual − line) per weekday and add half of it back,
 *       so a habit like "Sundays are heavier" shows up without over-reacting to one big day.</li>
 *   <li>The range (low–high) is the prediction ± one standard deviation of the remaining error.</li>
 * </ol>
 */
@Service
public class ForecastService {

    static final int HISTORY_DAYS = 28;
    static final int MIN_DAYS = 5;
    static final double WEEKDAY_WEIGHT = 0.5;

    private final FoodLogEntryRepository entries;
    private final ProfileService profileService;

    public ForecastService(FoodLogEntryRepository entries, ProfileService profileService) {
        this.entries = entries;
        this.profileService = profileService;
    }

    @Transactional
    public ForecastResponse forecast(int ahead) {
        int goal = profileService.get().getCalorieGoal();
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(HISTORY_DAYS);
        LocalDate end = today.minusDays(1);

        Map<LocalDate, Integer> perDay = new TreeMap<>(entries.findByLogDateBetween(start, end).stream()
                .collect(Collectors.groupingBy(FoodLogEntry::getLogDate,
                        Collectors.summingInt(FoodLogEntry::getCalories))));

        String method = "Linear trend + weekday pattern (least squares on the last " + HISTORY_DAYS + " days)";
        if (perDay.size() < MIN_DAYS) {
            return new ForecastResponse(false,
                    "Log at least " + MIN_DAYS + " days to unlock the forecast (" + perDay.size() + " so far).",
                    method, perDay.size(), goal, 0, 0, null, 0, List.of());
        }

        // x = days since start, y = calories
        double[] x = new double[perDay.size()];
        double[] y = new double[perDay.size()];
        DayOfWeek[] dow = new DayOfWeek[perDay.size()];
        int i = 0;
        for (Map.Entry<LocalDate, Integer> e : perDay.entrySet()) {
            x[i] = ChronoUnit.DAYS.between(start, e.getKey());
            y[i] = e.getValue();
            dow[i] = e.getKey().getDayOfWeek();
            i++;
        }
        int n = x.length;

        // 1) Least-squares line
        double meanX = Arrays.stream(x).average().orElse(0);
        double meanY = Arrays.stream(y).average().orElse(0);
        double sxy = 0, sxx = 0;
        for (int k = 0; k < n; k++) {
            sxy += (x[k] - meanX) * (y[k] - meanY);
            sxx += (x[k] - meanX) * (x[k] - meanX);
        }
        double slope = sxx == 0 ? 0 : sxy / sxx;
        // Keep the trend sensible: at most ±40 kcal/day so a few odd days can't explode the forecast.
        slope = Math.max(-40, Math.min(40, slope));
        double intercept = meanY - slope * meanX;

        // 2) Weekday adjustment from residuals
        Map<DayOfWeek, List<Double>> residuals = new EnumMap<>(DayOfWeek.class);
        for (int k = 0; k < n; k++) {
            residuals.computeIfAbsent(dow[k], d -> new ArrayList<>()).add(y[k] - (intercept + slope * x[k]));
        }
        Map<DayOfWeek, Double> weekdayAdj = new EnumMap<>(DayOfWeek.class);
        residuals.forEach((d, list) -> {
            if (list.size() >= 2) {
                weekdayAdj.put(d, WEEKDAY_WEIGHT * list.stream().mapToDouble(Double::doubleValue).average().orElse(0));
            }
        });

        // 3) Fit quality and spread
        double ssRes = 0, ssTot = 0;
        for (int k = 0; k < n; k++) {
            double fitted = intercept + slope * x[k] + weekdayAdj.getOrDefault(dow[k], 0.0);
            ssRes += Math.pow(y[k] - fitted, 2);
            ssTot += Math.pow(y[k] - meanY, 2);
        }
        double rSquared = ssTot == 0 ? 0 : Math.max(0, 1 - ssRes / ssTot);
        double sd = Math.sqrt(ssRes / Math.max(1, n - 2));

        // 4) Predict
        List<ForecastResponse.Point> points = new ArrayList<>();
        int overGoal = 0;
        long sum = 0;
        for (int d = 0; d < ahead; d++) {
            LocalDate date = today.plusDays(d + 1);
            double xd = ChronoUnit.DAYS.between(start, date);
            int predicted = (int) Math.max(0, Math.round(intercept + slope * xd
                    + weekdayAdj.getOrDefault(date.getDayOfWeek(), 0.0)));
            points.add(new ForecastResponse.Point(date, predicted,
                    (int) Math.max(0, Math.round(predicted - sd)), (int) Math.round(predicted + sd)));
            if (predicted > goal) overGoal++;
            sum += predicted;
        }
        int avg = (int) Math.round((double) sum / ahead);

        String message = overGoal == 0
                ? "On your current pattern you should stay within your goal all week."
                : "On your current pattern, " + overGoal + " of the next " + ahead + " days may go over your goal.";

        return new ForecastResponse(true, message, method, n, goal,
                LogService.round1(slope), Math.round(rSquared * 100) / 100.0, avg, overGoal, points);
    }
}
