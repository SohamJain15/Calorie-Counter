package com.caloriecounter.service;

import com.caloriecounter.dto.FoodRequest;
import com.caloriecounter.exception.BadRequestException;
import com.caloriecounter.exception.NotFoundException;
import com.caloriecounter.model.Food;
import com.caloriecounter.model.FoodCategory;
import com.caloriecounter.repository.FoodRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class FoodService {

    private final FoodRepository repository;

    public FoodService(FoodRepository repository) {
        this.repository = repository;
    }

    /** Search by name (case-insensitive, partial) and/or category. Both are optional. */
    @Transactional(readOnly = true)
    public List<Food> search(String query, String category) {
        FoodCategory cat = parseCategory(category);
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        return repository.findAll(Sort.by("id")).stream()
                .filter(f -> cat == null || f.getCategory() == cat)
                .filter(f -> q.isEmpty() || f.getName().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    @Transactional(readOnly = true)
    public Food get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Food " + id + " not found"));
    }

    @Transactional
    public Food create(FoodRequest r) {
        Food food = new Food(r.name().trim(), r.category(), r.servingSize().trim(), r.calories(),
                nz(r.protein()), nz(r.carbs()), nz(r.fat()), true);
        return repository.save(food);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
    }

    private static FoodCategory parseCategory(String category) {
        if (category == null || category.isBlank() || category.equalsIgnoreCase("all")) return null;
        try {
            return FoodCategory.fromJson(category);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown category '" + category
                    + "'. Use indian, fruits, vegetables, grains, dairy, meat or other");
        }
    }

    private static double nz(Double d) {
        return d == null ? 0 : d;
    }
}
