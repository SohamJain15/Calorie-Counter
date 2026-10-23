package com.caloriecounter.service;

import com.caloriecounter.dto.ProfileRequest;
import com.caloriecounter.model.UserProfile;
import com.caloriecounter.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserProfileRepository repository;

    public ProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    /** Returns the profile, creating one with default goals on first use. */
    @Transactional
    public UserProfile get() {
        return repository.findById(UserProfile.SINGLETON_ID)
                .orElseGet(() -> repository.save(new UserProfile()));
    }

    @Transactional
    public UserProfile update(ProfileRequest r) {
        UserProfile p = get();
        p.setName(r.name() == null || r.name().isBlank() ? null : r.name().trim());
        p.setAge(r.age());
        p.setSex(r.sex());
        p.setHeightCm(r.heightCm());
        p.setWeightKg(r.weightKg());
        p.setActivityLevel(r.activityLevel());
        p.setCalorieGoal(r.calorieGoal());
        p.setProteinGoal(r.proteinGoal());
        p.setCarbsGoal(r.carbsGoal());
        p.setFatGoal(r.fatGoal());
        return repository.save(p);
    }
}
