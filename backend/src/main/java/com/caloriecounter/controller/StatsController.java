package com.caloriecounter.controller;

import com.caloriecounter.dto.ForecastResponse;
import com.caloriecounter.dto.ProgressResponse;
import com.caloriecounter.dto.SuggestionResponse;
import com.caloriecounter.dto.SummaryResponse;
import com.caloriecounter.exception.BadRequestException;
import com.caloriecounter.service.ForecastService;
import com.caloriecounter.service.SampleDataService;
import com.caloriecounter.service.StatsService;
import com.caloriecounter.service.SuggestionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatsController {

    private final StatsService stats;
    private final ForecastService forecast;
    private final SuggestionService suggestions;
    private final SampleDataService sampleData;

    public StatsController(StatsService stats, ForecastService forecast,
                           SuggestionService suggestions, SampleDataService sampleData) {
        this.stats = stats;
        this.forecast = forecast;
        this.suggestions = suggestions;
        this.sampleData = sampleData;
    }

    /** GET /api/summary?date=2026-10-23 — dashboard totals (defaults to today). */
    @GetMapping("/summary")
    public SummaryResponse summary(@RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return stats.summary(date != null ? date : LocalDate.now());
    }

    /** GET /api/progress?days=7 — history ending today. days must be 1–365. */
    @GetMapping("/progress")
    public ProgressResponse progress(@RequestParam(defaultValue = "7") int days) {
        if (days < 1 || days > 365) {
            throw new BadRequestException("days must be between 1 and 365");
        }
        return stats.progress(days);
    }

    /** GET /api/forecast?days=7 — predicted calories for the next days, starting tomorrow (1–14). */
    @GetMapping("/forecast")
    public ForecastResponse forecast(@RequestParam(defaultValue = "7") int days) {
        if (days < 1 || days > 14) {
            throw new BadRequestException("days must be between 1 and 14");
        }
        return forecast.forecast(days);
    }

    /** GET /api/suggestions?date= — meal ideas that fit what is left of the day's budget. */
    @GetMapping("/suggestions")
    public SuggestionResponse suggestions(@RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return suggestions.suggest(date != null ? date : LocalDate.now());
    }

    /** POST /api/sample-data/week — fills the previous 7 days with example meals (skips days already logged). */
    @PostMapping("/sample-data/week")
    public Map<String, Integer> sampleWeek() {
        return Map.of("daysFilled", sampleData.fillPreviousWeek());
    }

    /** POST /api/sample-data?days=30 — fills the last N days plus today's past meals (skips days already logged). */
    @PostMapping("/sample-data")
    public Map<String, Integer> sample(@RequestParam(defaultValue = "30") int days) {
        if (days < 1 || days > 90) {
            throw new BadRequestException("days must be between 1 and 90");
        }
        return Map.of("daysFilled", sampleData.fill(days, true));
    }
}
