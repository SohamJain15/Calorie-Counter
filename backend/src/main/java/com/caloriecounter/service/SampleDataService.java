package com.caloriecounter.service;

import com.caloriecounter.model.ExerciseEntry;
import com.caloriecounter.model.Food;
import com.caloriecounter.model.FoodLogEntry;
import com.caloriecounter.model.MealType;
import com.caloriecounter.repository.ExerciseEntryRepository;
import com.caloriecounter.repository.FoodLogEntryRepository;
import com.caloriecounter.repository.FoodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Creates realistic example history so every page has something to show.
 * Weekends are a little heavier and older days slightly heavier than recent ones,
 * which gives the forecast a real pattern to learn.
 */
@Service
public class SampleDataService {

    /** Each option is a list of "Food name x quantity". */
    private static final List<List<String>> BREAKFASTS = List.of(
            List.of("Poha with Peanuts x1", "Masala Chai x1"),
            List.of("Idli x1.5", "Sambar x1"),
            List.of("Oats with Banana x1"),
            List.of("Aloo Paratha x1", "Plain Curd x0.5"),
            List.of("Plain Dosa x1", "Sambar x1", "Masala Chai x1"),
            List.of("Upma x1", "Masala Chai x1"),
            List.of("Egg Omelette x1", "Brown Bread x1"),
            List.of("Milk (Toned) x1", "Banana x1", "Almonds x1"));
    private static final List<List<String>> LUNCHES = List.of(
            List.of("Dal Tadka x1", "Steamed Rice x1", "Roti / Chapati x2"),
            List.of("Rajma Masala x1", "Steamed Rice x1.5"),
            List.of("Chole x1", "Roti / Chapati x2", "Cucumber Salad x1"),
            List.of("Chicken Rice Bowl x1"),
            List.of("Vegetable Biryani x1", "Plain Curd x1"),
            List.of("Palak Sabzi x1", "Roti / Chapati x3", "Buttermilk (Chaas) x1"),
            List.of("Sambar x1", "Brown Rice x1", "Mixed Vegetable Sabzi x1"));
    private static final List<List<String>> DINNERS = List.of(
            List.of("Paneer Butter Masala x1", "Roti / Chapati x2"),
            List.of("Dal Tadka x1", "Roti / Chapati x3", "Bhindi Fry x1"),
            List.of("Fish Curry x1", "Steamed Rice x1"),
            List.of("Paneer Wrap x1", "Mixed Vegetable Sabzi x1"),
            List.of("Chicken Breast x1.5", "Mixed Vegetable Sabzi x1", "Roti / Chapati x1"),
            List.of("Egg Omelette x1", "Roti / Chapati x2", "Cucumber Salad x1"));
    private static final List<List<String>> WEEKEND_DINNERS = List.of(
            List.of("Chicken Biryani x1.5", "Plain Curd x0.5"),
            List.of("Paneer Butter Masala x1.5", "Roti / Chapati x3"),
            List.of("Vegetable Biryani x1.5", "Vada Pav x1"));
    private static final List<List<String>> SNACKS = List.of(
            List.of("Banana x1"), List.of("Apple x1"), List.of("Peanuts (Roasted) x1"),
            List.of("Sprouts Salad x1"), List.of("Vada Pav x1"), List.of("Almonds x2"),
            List.of("Masala Chai x1", "Peanuts (Roasted) x1"), List.of("Papaya x1"), List.of("Buttermilk (Chaas) x1"));
    private static final List<String[]> WORKOUTS = List.of(
            new String[]{"Brisk walk", "5"}, new String[]{"Running", "10"}, new String[]{"Cycling", "8"},
            new String[]{"Yoga", "4"}, new String[]{"Badminton", "7"}, new String[]{"Gym workout", "6"});

    private final FoodRepository foods;
    private final FoodLogEntryRepository entries;
    private final ExerciseEntryRepository exercises;

    public SampleDataService(FoodRepository foods, FoodLogEntryRepository entries, ExerciseEntryRepository exercises) {
        this.foods = foods;
        this.entries = entries;
        this.exercises = exercises;
    }

    /** Previous 7 days only (kept for the Settings button and tests). */
    @Transactional
    public int fillPreviousWeek() {
        return fill(7, false);
    }

    /**
     * Fills the last {@code days} days (and optionally the meals of today that are already past).
     * Days that already have food logged are left alone.
     *
     * @return number of days that got data
     */
    @Transactional
    public int fill(int days, boolean includeToday) {
        Map<String, Food> byName = foods.findAll().stream()
                .collect(Collectors.toMap(Food::getName, Function.identity(), (a, b) -> a));
        int filled = 0;

        for (int i = includeToday ? 0 : 1; i <= days; i++) {
            LocalDate date = LocalDate.now().minusDays(i);
            if (entries.existsByLogDate(date)) continue;
            Random rnd = new Random(date.toEpochDay() * 31 + 7);
            boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
            int hourNow = i == 0 ? LocalTime.now().getHour() : 24;

            if (hourNow >= 9) add(date, MealType.BREAKFAST, LocalTime.of(8, 10 + rnd.nextInt(40)), pick(BREAKFASTS, rnd), byName);
            if (hourNow >= 14) add(date, MealType.LUNCH, LocalTime.of(13, rnd.nextInt(50)), pick(LUNCHES, rnd), byName);
            // Older days (and weekends) get a second snack more often, so intake trends slightly down over time.
            if (hourNow >= 18) {
                add(date, MealType.SNACK, LocalTime.of(17, rnd.nextInt(45)), pick(SNACKS, rnd), byName);
                if (rnd.nextDouble() < (weekend ? 0.6 : 0.15 + i * 0.012)) {
                    add(date, MealType.SNACK, LocalTime.of(11, 15 + rnd.nextInt(30)), pick(SNACKS, rnd), byName);
                }
            }
            if (hourNow >= 21) {
                add(date, MealType.DINNER, LocalTime.of(20, 5 + rnd.nextInt(50)),
                        weekend ? pick(WEEKEND_DINNERS, rnd) : pick(DINNERS, rnd), byName);
            }

            if (i > 0 && rnd.nextDouble() < (weekend ? 0.35 : 0.6)) {
                String[] w = pick(WORKOUTS, rnd);
                int minutes = 20 + rnd.nextInt(5) * 5;
                ExerciseEntry ex = new ExerciseEntry(date);
                ex.apply(date, LocalTime.of(6 + rnd.nextInt(2), rnd.nextInt(4) * 15), w[0], minutes,
                        minutes * Integer.parseInt(w[1]));
                exercises.save(ex);
            }
            filled++;
        }
        return filled;
    }

    private void add(LocalDate date, MealType meal, LocalTime time, List<String> items, Map<String, Food> byName) {
        for (String item : items) {
            int x = item.lastIndexOf(" x");
            Food f = byName.get(item.substring(0, x));
            if (f == null) continue;
            double q = Double.parseDouble(item.substring(x + 2));
            FoodLogEntry e = new FoodLogEntry(date);
            e.apply(date, meal, time, f.getId(), f.getName(), f.getServingSize(), q,
                    (int) Math.round(f.getCalories() * q),
                    LogService.round1(f.getProtein() * q), LogService.round1(f.getCarbs() * q),
                    LogService.round1(f.getFat() * q));
            entries.save(e);
        }
    }

    private static <T> T pick(List<T> options, Random rnd) {
        return options.get(rnd.nextInt(options.size()));
    }
}
