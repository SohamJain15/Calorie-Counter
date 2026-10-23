package com.caloriecounter.controller;

import com.caloriecounter.dto.ProfileRequest;
import com.caloriecounter.model.UserProfile;
import com.caloriecounter.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Profile and daily nutrition goals. */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping
    public UserProfile get() {
        return service.get();
    }

    @PutMapping
    public UserProfile update(@Valid @RequestBody ProfileRequest request) {
        return service.update(request);
    }
}
