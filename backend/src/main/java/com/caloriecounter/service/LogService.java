package com.caloriecounter.service;

import com.caloriecounter.dto.DayLogResponse;
import com.caloriecounter.dto.ExerciseRequest;
import com.caloriecounter.dto.LogEntryRequest;
import com.caloriecounter.exception.BadRequestException;
import com.caloriecounter.exception.NotFoundException;
import com.caloriecounter.model.ExerciseEntry;
import com.caloriecounter.model.Food;
import com.caloriecounter.model.FoodLogEntry;
import com.caloriecounter.repository.ExerciseEntryRepository;
import com.caloriecounter.repository.FoodLogEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Service
public class LogService {

    private final FoodLogEntryRepository entries;
    private final ExerciseEntryRepository exercises;
    private final FoodService foodService;

    public LogService(FoodLogEntryRepository entries, ExerciseEntryRepository exercises, FoodService foodService) {
        this.entries = entries;
        this.exercises = exercises;
        this.foodService = foodService;
    }

    @Transactional(readOnly = true)
    public DayLogResponse getDay(LocalDate date) {
        return new DayLogResponse(date,
                entries.findByLogDateOrderByLoggedTimeAscIdAsc(date),
                exercises.findByLogDateOrderByLoggedTimeAscIdAsc(date));
    }

    // ---------- Food entries ----------

    @Transactional
    public FoodLogEntry addEntry(LogEntryRequest r) {
        FoodLogEntry entry = new FoodLogEntry(LocalDate.now());
        fill(entry, r, LocalDate.now(), now());
        return entries.save(entry);
    }

    @Transactional
    public FoodLogEntry updateEntry(Long id, LogEntryRequest r) {
        FoodLogEntry entry = findEntry(id);
        fill(entry, r, entry.getLogDate(), entry.getLoggedTime());
        return entries.save(entry);
    }

    @Transactional
    public void deleteEntry(Long id) {
        entries.delete(findEntry(id));
    }

    /** Works out name, serving and nutrition totals from the food database or from the custom values. */
    private void fill(FoodLogEntry e, LogEntryRequest r, LocalDate defaultDate, LocalTime defaultTime) {
        LocalDate date = r.date() != null ? r.date() : defaultDate;
        LocalTime time = r.time() != null ? r.time().truncatedTo(ChronoUnit.MINUTES) : defaultTime;
        double q = r.quantity();

        if (r.foodId() != null) {
            Food f = foodService.get(r.foodId());
            e.apply(date, r.mealType(), time, f.getId(), f.getName(), f.getServingSize(), q,
                    (int) Math.round(f.getCalories() * q),
                    round1(f.getProtein() * q), round1(f.getCarbs() * q), round1(f.getFat() * q));
            return;
        }

        if (r.name() == null || r.name().isBlank()) {
            throw new BadRequestException("Enter a food name, or pick a food from the database");
        }
        if (r.calories() == null) {
            throw new BadRequestException("Enter the calories for one serving of " + r.name().trim());
        }
        String serving = (r.servingSize() == null || r.servingSize().isBlank()) ? "1 serving" : r.servingSize().trim();
        e.apply(date, r.mealType(), time, null, r.name().trim(), serving, q,
                (int) Math.round(r.calories() * q),
                round1(nz(r.protein()) * q), round1(nz(r.carbs()) * q), round1(nz(r.fat()) * q));
    }

    private FoodLogEntry findEntry(Long id) {
        return entries.findById(id)
                .orElseThrow(() -> new NotFoundException("Food log entry " + id + " not found"));
    }

    // ---------- Exercise ----------

    @Transactional
    public ExerciseEntry addExercise(ExerciseRequest r) {
        ExerciseEntry e = new ExerciseEntry(LocalDate.now());
        fill(e, r, LocalDate.now(), now());
        return exercises.save(e);
    }

    @Transactional
    public ExerciseEntry updateExercise(Long id, ExerciseRequest r) {
        ExerciseEntry e = findExercise(id);
        fill(e, r, e.getLogDate(), e.getLoggedTime());
        return exercises.save(e);
    }

    @Transactional
    public void deleteExercise(Long id) {
        exercises.delete(findExercise(id));
    }

    private void fill(ExerciseEntry e, ExerciseRequest r, LocalDate defaultDate, LocalTime defaultTime) {
        e.apply(r.date() != null ? r.date() : defaultDate,
                r.time() != null ? r.time().truncatedTo(ChronoUnit.MINUTES) : defaultTime,
                r.name().trim(), r.durationMinutes(), r.caloriesBurned());
    }

    private ExerciseEntry findExercise(Long id) {
        return exercises.findById(id)
                .orElseThrow(() -> new NotFoundException("Exercise entry " + id + " not found"));
    }

    // ---------- Bulk delete ----------

    @Transactional
    public void clearDay(LocalDate date) {
        entries.deleteByLogDate(date);
        exercises.deleteByLogDate(date);
    }

    @Transactional
    public void clearAll() {
        entries.deleteAllInBatch();
        exercises.deleteAllInBatch();
    }

    // ---------- helpers ----------

    private static LocalTime now() {
        return LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
    }

    static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double nz(Double d) {
        return d == null ? 0 : d;
    }
}
