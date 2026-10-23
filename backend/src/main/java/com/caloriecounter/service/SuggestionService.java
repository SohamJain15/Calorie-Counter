package com.caloriecounter.service;

import com.caloriecounter.dto.SuggestionResponse;
import com.caloriecounter.dto.SummaryResponse;
import com.caloriecounter.model.Food;
import com.caloriecounter.model.MealType;
import com.caloriecounter.repository.FoodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Suggests what to eat next so the day lands close to the calorie goal with as much of the
 * remaining protein covered as possible.
 *
 * <ol>
 *   <li>Work out the next meal (first meal not logged yet, or by time of day).</li>
 *   <li>Calorie target = what is left today, capped at a normal size for that meal.</li>
 *   <li>Build real meals from foods suitable for that meal: a curry/sabzi with roti, rice or bread,
 *       or a complete dish (biryani, dosa, poha…) on its own, optionally with a light side (curd, salad, chaas).</li>
 *   <li>Keep candidates between 55% and 100% of the target, then score them:
 *       60% protein coverage + 40% calorie fit, minus a penalty for very fatty options.</li>
 *   <li>Return the top 3 with different main foods.</li>
 * </ol>
 */
@Service
public class SuggestionService {

    private static final Map<MealType, Integer> MEAL_CAP = Map.of(
            MealType.BREAKFAST, 500, MealType.LUNCH, 700, MealType.DINNER, 700, MealType.SNACK, 250);

    private static final Map<MealType, Set<String>> SUITABLE = Map.of(
            MealType.BREAKFAST, Set.of("Poha with Peanuts", "Idli", "Plain Dosa", "Upma", "Aloo Paratha", "Oats with Banana",
                    "Brown Bread", "Boiled Egg", "Egg Omelette", "Milk (Toned)", "Plain Curd", "Masala Chai", "Banana",
                    "Apple", "Papaya", "Sambar", "Sprouts Salad", "Almonds", "Paneer"),
            MealType.SNACK, Set.of("Banana", "Apple", "Mango", "Papaya", "Orange", "Grapes", "Sprouts Salad",
                    "Peanuts (Roasted)", "Almonds", "Buttermilk (Chaas)", "Masala Chai", "Boiled Egg", "Plain Curd",
                    "Vada Pav", "Cucumber Salad", "Milk (Toned)"),
            MealType.LUNCH, Set.of("Dal Tadka", "Steamed Rice", "Brown Rice", "Roti / Chapati", "Rajma Masala", "Chole",
                    "Paneer Butter Masala", "Chicken Breast", "Fish Curry", "Vegetable Biryani", "Chicken Biryani",
                    "Mixed Vegetable Sabzi", "Palak Sabzi", "Bhindi Fry", "Cucumber Salad", "Plain Curd",
                    "Buttermilk (Chaas)", "Paneer Wrap", "Egg Omelette", "Sambar", "Chicken Rice Bowl", "Paneer",
                    "Sprouts Salad"));

    /** Rice, roti, bread: eaten with a curry/sabzi, never suggested on their own. */
    private static final Set<String> STAPLES = Set.of("Roti / Chapati", "Steamed Rice", "Brown Rice", "Brown Bread");

    /** Small extras that can go with any dish. */
    private static final Set<String> LIGHT_SIDES = Set.of("Cucumber Salad", "Buttermilk (Chaas)", "Plain Curd",
            "Masala Chai", "Sambar");

    /** Dishes that need a staple (roti / rice / bread) to make a meal. */
    private static final Set<String> CURRIES = Set.of("Dal Tadka", "Rajma Masala", "Chole", "Paneer Butter Masala",
            "Fish Curry", "Mixed Vegetable Sabzi", "Palak Sabzi", "Bhindi Fry", "Chicken Breast", "Paneer", "Egg Omelette");

    private final FoodRepository foods;
    private final StatsService stats;

    public SuggestionService(FoodRepository foods, StatsService stats) {
        this.foods = foods;
        this.stats = stats;
    }

    @Transactional
    public SuggestionResponse suggest(LocalDate date) {
        SummaryResponse s = stats.summary(date);
        MealType meal = nextMeal(s, date);
        int remaining = s.remaining();
        double proteinLeft = Math.max(0, s.proteinGoal() - s.protein());

        if (remaining < 80) {
            List<SuggestionResponse.Suggestion> light = lightOptions();
            return new SuggestionResponse(date, MealType.SNACK, remaining, LogService.round1(proteinLeft), 0,
                    remaining < 0
                            ? "You’re " + (-remaining) + " kcal over today. If you’re still hungry, keep it light:"
                            : "You’ve nearly used today’s budget. If you’re hungry, keep it light:",
                    light);
        }

        int target = Math.min(remaining, MEAL_CAP.get(meal));
        int low = (int) (target * 0.55);
        List<Food> pool = foods.findAll().stream().filter(f -> suits(f, meal)).toList();

        List<Food> staples = pool.stream().filter(f -> STAPLES.contains(f.getName())).toList();
        List<Food> lightSides = pool.stream().filter(f -> LIGHT_SIDES.contains(f.getName())).toList();

        List<Candidate> candidates = new ArrayList<>();
        for (Food f : pool) {
            String name = f.getName();
            if (STAPLES.contains(name) || (LIGHT_SIDES.contains(name) && meal != MealType.SNACK)) continue;

            List<List<Pick>> bases = new ArrayList<>();
            if (CURRIES.contains(name)) {
                // curry + staple: 2 or 3 roti, 1 bowl of rice, 2 slices of bread
                for (Food st : staples) {
                    if (st.getName().startsWith("Roti")) {
                        bases.add(List.of(new Pick(f, 1), new Pick(st, 2)));
                        bases.add(List.of(new Pick(f, 1), new Pick(st, 3)));
                    } else {
                        bases.add(List.of(new Pick(f, 1), new Pick(st, 1)));
                    }
                }
                if (meal == MealType.BREAKFAST || meal == MealType.SNACK) bases.add(List.of(new Pick(f, 1)));
            } else {
                // complete dish: on its own, or 2 servings if small
                bases.add(List.of(new Pick(f, 1)));
                if (f.getCalories() <= 150) bases.add(List.of(new Pick(f, 2)));
            }

            for (List<Pick> base : bases) {
                candidates.add(new Candidate(base));
                if (meal != MealType.SNACK) {
                    for (Food side : lightSides) {
                        List<Pick> withSide = new ArrayList<>(base);
                        withSide.add(new Pick(side, 1));
                        candidates.add(new Candidate(withSide));
                    }
                }
            }
        }

        List<Candidate> ranked = candidates.stream()
                .filter(c -> c.kcal() >= low && c.kcal() <= target)
                .sorted(Comparator.comparingDouble((Candidate c) -> c.score(target, proteinLeft)).reversed())
                .toList();

        // Top 3 with different main foods
        List<SuggestionResponse.Suggestion> out = new ArrayList<>();
        Set<String> usedMains = new HashSet<>();
        for (Candidate c : ranked) {
            if (!usedMains.add(c.picks.get(0).food.getName())) continue;
            out.add(c.toSuggestion(remaining, proteinLeft));
            if (out.size() == 3) break;
        }

        String message = "You have " + remaining + " kcal and " + Math.round(proteinLeft) + "g protein left today. Ideas for "
                + meal.toJson() + ":";
        return new SuggestionResponse(date, meal, remaining, LogService.round1(proteinLeft), target, message, out);
    }

    private static boolean suits(Food f, MealType meal) {
        if (f.isCustom()) return true;
        MealType key = meal == MealType.DINNER ? MealType.LUNCH : meal;
        return SUITABLE.get(key).contains(f.getName());
    }

    /** Next meal: the first one not logged yet, but never earlier than the time of day suggests. */
    private static MealType nextMeal(SummaryResponse s, LocalDate date) {
        MealType byTime = MealType.BREAKFAST;
        if (date.equals(LocalDate.now())) {
            int h = LocalTime.now().getHour();
            byTime = h < 11 ? MealType.BREAKFAST : h < 16 ? MealType.LUNCH : h < 19 ? MealType.SNACK : MealType.DINNER;
        }
        List<MealType> order = List.of(MealType.BREAKFAST, MealType.LUNCH, MealType.SNACK, MealType.DINNER);
        for (MealType m : order.subList(order.indexOf(byTime), order.size())) {
            boolean logged = s.meals().stream().anyMatch(ms -> ms.mealType() == m && ms.items() > 0);
            if (!logged) return m;
        }
        return MealType.SNACK;
    }

    private List<SuggestionResponse.Suggestion> lightOptions() {
        return foods.findAll().stream()
                .filter(f -> f.getCalories() <= 150 && !f.isCustom())
                .sorted(Comparator.comparingDouble((Food f) -> f.getProtein() / Math.max(1, f.getCalories())).reversed())
                .limit(3)
                .map(f -> new Candidate(List.of(new Pick(f, 1))).toSuggestion(0, 0))
                .toList();
    }

    // ---------- candidate helpers ----------

    private record Pick(Food food, double qty) {
    }

    private static final class Candidate {
        final List<Pick> picks;

        Candidate(List<Pick> picks) {
            this.picks = picks;
        }

        int kcal() {
            return (int) Math.round(picks.stream().mapToDouble(p -> p.food.getCalories() * p.qty).sum());
        }

        double protein() { return picks.stream().mapToDouble(p -> p.food.getProtein() * p.qty).sum(); }
        double carbs() { return picks.stream().mapToDouble(p -> p.food.getCarbs() * p.qty).sum(); }
        double fat() { return picks.stream().mapToDouble(p -> p.food.getFat() * p.qty).sum(); }

        double score(int target, double proteinLeft) {
            double proteinScore = proteinLeft <= 0 ? 0.5 : Math.min(protein(), proteinLeft) / proteinLeft;
            double fit = 1 - Math.abs(kcal() - target * 0.85) / target;
            double fatShare = kcal() == 0 ? 0 : fat() * 9 / kcal();
            double fatPenalty = fatShare > 0.4 ? (fatShare - 0.4) : 0;
            return 0.6 * proteinScore + 0.4 * fit - fatPenalty;
        }

        SuggestionResponse.Suggestion toSuggestion(int remaining, double proteinLeft) {
            String title = String.join(" + ", picks.stream().map(p ->
                    (p.qty == 1 ? "" : (int) p.qty + " × ") + p.food.getName()).toList());
            List<SuggestionResponse.Item> items = picks.stream().map(p -> new SuggestionResponse.Item(
                    p.food.getId(), p.food.getName(), p.qty, p.food.getServingSize(),
                    (int) Math.round(p.food.getCalories() * p.qty))).toList();

            String reason;
            if (remaining <= 0) {
                reason = "Only " + kcal() + " kcal with " + LogService.round1(protein()) + "g protein.";
            } else {
                long left = remaining - kcal();
                reason = "Leaves you " + left + " kcal for the rest of the day"
                        + (proteinLeft > 0
                        ? " and covers " + Math.round(Math.min(protein(), proteinLeft)) + "g of the "
                        + Math.round(proteinLeft) + "g protein you still need."
                        : ".");
            }
            return new SuggestionResponse.Suggestion(title, items, kcal(), LogService.round1(protein()),
                    LogService.round1(carbs()), LogService.round1(fat()), reason);
        }
    }
}
