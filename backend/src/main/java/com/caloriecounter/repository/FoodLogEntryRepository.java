package com.caloriecounter.repository;

import com.caloriecounter.model.FoodLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FoodLogEntryRepository extends JpaRepository<FoodLogEntry, Long> {

    List<FoodLogEntry> findByLogDateOrderByLoggedTimeAscIdAsc(LocalDate logDate);

    List<FoodLogEntry> findByLogDateBetween(LocalDate from, LocalDate to);

    boolean existsByLogDate(LocalDate logDate);

    void deleteByLogDate(LocalDate logDate);
}
