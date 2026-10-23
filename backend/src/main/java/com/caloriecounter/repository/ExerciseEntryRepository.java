package com.caloriecounter.repository;

import com.caloriecounter.model.ExerciseEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExerciseEntryRepository extends JpaRepository<ExerciseEntry, Long> {

    List<ExerciseEntry> findByLogDateOrderByLoggedTimeAscIdAsc(LocalDate logDate);

    List<ExerciseEntry> findByLogDateBetween(LocalDate from, LocalDate to);

    void deleteByLogDate(LocalDate logDate);
}
