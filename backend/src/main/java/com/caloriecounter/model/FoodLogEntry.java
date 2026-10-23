package com.caloriecounter.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** One food the user ate. Calories and macros are totals for the logged quantity. */
@Entity
@Table(name = "food_log_entries", indexes = @Index(name = "idx_food_log_date", columnList = "logDate"))
public class FoodLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate logDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MealType mealType;

    @Column(nullable = false)
    private LocalTime loggedTime;

    /** Food database item this came from, or null for a one-off custom entry. */
    private Long foodId;

    @Column(nullable = false, length = 100)
    private String foodName;

    /** Serving description of ONE unit, e.g. "1 bowl". */
    @Column(nullable = false, length = 50)
    private String servingSize;

    /** Number of servings eaten. */
    @Column(nullable = false)
    private double quantity;

    @Column(nullable = false)
    private int calories;

    @Column(nullable = false)
    private double protein;

    @Column(nullable = false)
    private double carbs;

    @Column(nullable = false)
    private double fat;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected FoodLogEntry() {
    }

    public FoodLogEntry(LocalDate logDate) {
        this.logDate = logDate;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    /** Sets everything except the id/createdAt in one go (used for both create and update). */
    public void apply(LocalDate logDate, MealType mealType, LocalTime loggedTime, Long foodId, String foodName,
                      String servingSize, double quantity, int calories, double protein, double carbs, double fat) {
        this.logDate = logDate;
        this.mealType = mealType;
        this.loggedTime = loggedTime;
        this.foodId = foodId;
        this.foodName = foodName;
        this.servingSize = servingSize;
        this.quantity = quantity;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
    }

    public Long getId() { return id; }
    public LocalDate getLogDate() { return logDate; }
    public MealType getMealType() { return mealType; }
    public LocalTime getLoggedTime() { return loggedTime; }
    public Long getFoodId() { return foodId; }
    public String getFoodName() { return foodName; }
    public String getServingSize() { return servingSize; }
    public double getQuantity() { return quantity; }
    public int getCalories() { return calories; }
    public double getProtein() { return protein; }
    public double getCarbs() { return carbs; }
    public double getFat() { return fat; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
