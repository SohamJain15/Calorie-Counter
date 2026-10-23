package com.caloriecounter.controller;

import com.caloriecounter.dto.FoodRequest;
import com.caloriecounter.model.Food;
import com.caloriecounter.service.FoodService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Food database: browse/search foods and add custom ones. */
@RestController
@RequestMapping("/api/foods")
public class FoodController {

    private final FoodService service;

    public FoodController(FoodService service) {
        this.service = service;
    }

    /** GET /api/foods?q=paneer&category=dairy — both optional. */
    @GetMapping
    public List<Food> search(@RequestParam(required = false) String q,
                             @RequestParam(required = false) String category) {
        return service.search(q, category);
    }

    @GetMapping("/{id}")
    public Food get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Food create(@Valid @RequestBody FoodRequest request) {
        return service.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
