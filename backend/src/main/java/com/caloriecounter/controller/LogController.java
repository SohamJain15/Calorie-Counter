package com.caloriecounter.controller;

import com.caloriecounter.dto.DayLogResponse;
import com.caloriecounter.dto.ExerciseRequest;
import com.caloriecounter.dto.LogEntryRequest;
import com.caloriecounter.model.ExerciseEntry;
import com.caloriecounter.model.FoodLogEntry;
import com.caloriecounter.service.LogService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Daily food log and exercise log. */
@RestController
@RequestMapping("/api/log")
public class LogController {

    private final LogService service;

    public LogController(LogService service) {
        this.service = service;
    }

    /** GET /api/log?date=2026-10-23 (defaults to today). */
    @GetMapping
    public DayLogResponse day(@RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getDay(date != null ? date : LocalDate.now());
    }

    // ---------- Food entries ----------

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public FoodLogEntry addEntry(@Valid @RequestBody LogEntryRequest request) {
        return service.addEntry(request);
    }

    @PutMapping("/entries/{id}")
    public FoodLogEntry updateEntry(@PathVariable Long id, @Valid @RequestBody LogEntryRequest request) {
        return service.updateEntry(id, request);
    }

    @DeleteMapping("/entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEntry(@PathVariable Long id) {
        service.deleteEntry(id);
    }

    // ---------- Exercise ----------

    @PostMapping("/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseEntry addExercise(@Valid @RequestBody ExerciseRequest request) {
        return service.addExercise(request);
    }

    @PutMapping("/exercises/{id}")
    public ExerciseEntry updateExercise(@PathVariable Long id, @Valid @RequestBody ExerciseRequest request) {
        return service.updateExercise(id, request);
    }

    @DeleteMapping("/exercises/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExercise(@PathVariable Long id) {
        service.deleteExercise(id);
    }

    // ---------- Bulk ----------

    /** Removes every food and exercise entry on one day. */
    @DeleteMapping("/day/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearDay(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        service.clearDay(date);
    }

    /** Removes the whole food and exercise history (the food database and profile are kept). */
    @DeleteMapping("/all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearAll() {
        service.clearAll();
    }
}
