package com.caloriecounter.model;

import jakarta.persistence.*;

/** Single-user profile and nutrition goals. Always stored with id = 1. */
@Entity
@Table(name = "user_profile")
public class UserProfile {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(length = 60)
    private String name;

    private Integer age;

    /** "male" or "female" — only used to estimate a calorie goal. */
    @Column(length = 10)
    private String sex;

    private Double heightCm;

    private Double weightKg;

    /** sedentary | light | moderate | active | very_active */
    @Column(length = 20)
    private String activityLevel;

    @Column(nullable = false)
    private int calorieGoal = 2200;

    @Column(nullable = false)
    private int proteinGoal = 140;

    @Column(nullable = false)
    private int carbsGoal = 250;

    @Column(nullable = false)
    private int fatGoal = 70;

    public UserProfile() {
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }
    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }
    public int getCalorieGoal() { return calorieGoal; }
    public void setCalorieGoal(int calorieGoal) { this.calorieGoal = calorieGoal; }
    public int getProteinGoal() { return proteinGoal; }
    public void setProteinGoal(int proteinGoal) { this.proteinGoal = proteinGoal; }
    public int getCarbsGoal() { return carbsGoal; }
    public void setCarbsGoal(int carbsGoal) { this.carbsGoal = carbsGoal; }
    public int getFatGoal() { return fatGoal; }
    public void setFatGoal(int fatGoal) { this.fatGoal = fatGoal; }
}
