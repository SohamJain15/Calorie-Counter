package com.caloriecounter.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Exercise logged on a day. Calories burned are added back to the day's budget. */
@Entity
@Table(name = "exercise_entries", indexes = @Index(name = "idx_exercise_date", columnList = "logDate"))
public class ExerciseEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate logDate;

    @Column(nullable = false)
    private LocalTime loggedTime;

    @Column(nullable = false, length = 100)
    private String name;

    private Integer durationMinutes;

    @Column(nullable = false)
    private int caloriesBurned;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ExerciseEntry() {
    }

    public ExerciseEntry(LocalDate logDate) {
        this.logDate = logDate;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public void apply(LocalDate logDate, LocalTime loggedTime, String name, Integer durationMinutes, int caloriesBurned) {
        this.logDate = logDate;
        this.loggedTime = loggedTime;
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.caloriesBurned = caloriesBurned;
    }

    public Long getId() { return id; }
    public LocalDate getLogDate() { return logDate; }
    public LocalTime getLoggedTime() { return loggedTime; }
    public String getName() { return name; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public int getCaloriesBurned() { return caloriesBurned; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
