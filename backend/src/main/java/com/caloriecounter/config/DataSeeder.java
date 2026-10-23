package com.caloriecounter.config;

import com.caloriecounter.model.Food;
import com.caloriecounter.model.FoodCategory;
import com.caloriecounter.repository.FoodLogEntryRepository;
import com.caloriecounter.repository.FoodRepository;
import com.caloriecounter.service.ProfileService;
import com.caloriecounter.service.SampleDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.caloriecounter.model.FoodCategory.*;

/** Fills the food database with common Indian-friendly foods the first time the app starts. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final FoodRepository foods;
    private final FoodLogEntryRepository entries;
    private final ProfileService profileService;
    private final SampleDataService sampleData;

    /** When true (default) and the food log is empty, 30 days of example history are created on startup. */
    @Value("${app.sample-data.on-empty:true}")
    private boolean sampleOnEmpty;

    public DataSeeder(FoodRepository foods, FoodLogEntryRepository entries,
                      ProfileService profileService, SampleDataService sampleData) {
        this.foods = foods;
        this.entries = entries;
        this.profileService = profileService;
        this.sampleData = sampleData;
    }

    @Override
    public void run(String... args) {
        profileService.get(); // creates the default profile if missing
        if (foods.count() == 0) {
            foods.saveAll(defaultFoods());
        }
        if (sampleOnEmpty && entries.count() == 0) {
            sampleData.fill(30, true);
        }
    }

    /** Estimated nutrition per serving: calories, protein g, carbs g, fat g. */
    static List<Food> defaultFoods() {
        return List.of(
                f("Banana", FRUITS, "1 medium", 105, 1.3, 27, 0.3),
                f("Roti / Chapati", INDIAN, "1 piece", 120, 3.5, 18, 3.5),
                f("Dal Tadka", INDIAN, "1 cup", 230, 12, 32, 6),
                f("Paneer", DAIRY, "100g", 265, 18, 6, 20),
                f("Steamed Rice", GRAINS, "1 cup", 205, 4.3, 44.5, 0.4),
                f("Poha with Peanuts", INDIAN, "1 bowl", 250, 6, 38, 8),
                f("Idli", INDIAN, "2 pieces", 140, 4, 28, 1),
                f("Plain Dosa", INDIAN, "1 medium", 168, 3.9, 29, 4.5),
                f("Plain Curd", DAIRY, "1 cup", 149, 8.5, 11, 8),
                f("Chicken Breast", MEAT, "100g", 165, 31, 0, 3.6),
                f("Chole", INDIAN, "1 cup", 270, 14, 39, 7),
                f("Rajma Masala", INDIAN, "1 cup", 240, 13, 36, 5),
                f("Sambar", INDIAN, "1 cup", 150, 7, 22, 4),
                f("Upma", INDIAN, "1 bowl", 220, 5, 32, 8),
                f("Aloo Paratha", INDIAN, "1 piece", 290, 6, 40, 12),
                f("Vegetable Biryani", INDIAN, "1 plate", 380, 8, 58, 12),
                f("Chicken Biryani", INDIAN, "1 plate", 450, 25, 52, 15),
                f("Paneer Butter Masala", INDIAN, "1 cup", 400, 16, 14, 32),
                f("Paneer Wrap", INDIAN, "1 wrap", 340, 15, 36, 15),
                f("Vada Pav", INDIAN, "1 piece", 290, 6, 42, 11),
                f("Masala Chai", INDIAN, "1 cup", 90, 3, 12, 3.5),
                f("Oats with Banana", GRAINS, "1 bowl", 320, 10, 58, 6),
                f("Brown Rice", GRAINS, "1 cup", 216, 5, 45, 1.8),
                f("Brown Bread", GRAINS, "2 slices", 150, 6, 26, 2),
                f("Apple", FRUITS, "1 medium", 95, 0.5, 25, 0.3),
                f("Mango", FRUITS, "1 cup", 99, 1.4, 25, 0.6),
                f("Papaya", FRUITS, "1 cup", 62, 0.7, 16, 0.4),
                f("Orange", FRUITS, "1 medium", 62, 1.2, 15, 0.2),
                f("Grapes", FRUITS, "1 cup", 104, 1.1, 27, 0.2),
                f("Mixed Vegetable Sabzi", VEGETABLES, "1 cup", 150, 4, 16, 8),
                f("Palak Sabzi", VEGETABLES, "1 cup", 120, 5, 10, 7),
                f("Bhindi Fry", VEGETABLES, "1 cup", 160, 3, 14, 11),
                f("Cucumber Salad", VEGETABLES, "1 bowl", 30, 1, 6, 0.2),
                f("Sprouts Salad", VEGETABLES, "1 bowl", 140, 10, 22, 1.5),
                f("Milk (Toned)", DAIRY, "1 glass", 145, 8, 12, 7.5),
                f("Buttermilk (Chaas)", DAIRY, "1 glass", 40, 2, 5, 1.5),
                f("Boiled Egg", MEAT, "1 large", 78, 6.3, 0.6, 5.3),
                f("Egg Omelette", MEAT, "2 eggs", 190, 13, 2, 15),
                f("Fish Curry", MEAT, "1 cup", 250, 24, 6, 14),
                f("Chicken Rice Bowl", MEAT, "1 bowl", 520, 32, 60, 14),
                f("Peanuts (Roasted)", OTHER, "30g", 170, 7, 5, 14),
                f("Almonds", OTHER, "10 pieces", 70, 2.6, 2.4, 6)
        );
    }

    private static Food f(String name, FoodCategory c, String serving, int kcal, double p, double carbs, double fat) {
        return new Food(name, c, serving, kcal, p, carbs, fat, false);
    }
}
