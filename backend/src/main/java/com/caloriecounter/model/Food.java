package com.caloriecounter.model;

import jakarta.persistence.*;

/** An item in the food database. Nutrition values are per one serving. */
@Entity
@Table(name = "foods")
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FoodCategory category;

    /** Human serving description, e.g. "1 cup", "100g", "2 pieces". */
    @Column(nullable = false, length = 50)
    private String servingSize;

    @Column(nullable = false)
    private int calories;

    @Column(nullable = false)
    private double protein;

    @Column(nullable = false)
    private double carbs;

    @Column(nullable = false)
    private double fat;

    /** true when the user added it themselves (not part of the built-in list). */
    @Column(nullable = false)
    private boolean custom;

    protected Food() {
    }

    public Food(String name, FoodCategory category, String servingSize, int calories,
                double protein, double carbs, double fat, boolean custom) {
        this.name = name;
        this.category = category;
        this.servingSize = servingSize;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.custom = custom;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public FoodCategory getCategory() { return category; }
    public String getServingSize() { return servingSize; }
    public int getCalories() { return calories; }
    public double getProtein() { return protein; }
    public double getCarbs() { return carbs; }
    public double getFat() { return fat; }
    public boolean isCustom() { return custom; }
}
