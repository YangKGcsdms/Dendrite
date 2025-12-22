package com.carter.controller;

import com.carter.dto.ApiResponse;
import com.carter.entity.TalentProfile;
import com.carter.repo.TalentProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST API for talent profile management.
 * Provides CRUD operations for viewing talent data.
 *
 * @author Carter
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private final TalentProfileRepository profileRepo;

    public ProfileController(TalentProfileRepository profileRepo) {
        this.profileRepo = profileRepo;
    }

    /**
     * Lists all talent profiles with pagination.
     *
     * @param page page number (0-indexed)
     * @param size page size
     * @return paginated list of profiles
     */
    @GetMapping
    public ApiResponse<Page<TalentProfile>> listProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("lastUpdated").descending());
        Page<TalentProfile> profiles = profileRepo.findAll(pageRequest);

        return ApiResponse.success(profiles);
    }

    /**
     * Gets a specific talent profile by employee name.
     *
     * @param name employee name
     * @return profile if found
     */
    @GetMapping("/{name}")
    public ApiResponse<TalentProfile> getProfile(@PathVariable String name) {
        Optional<TalentProfile> profile = profileRepo.findByEmployeeName(name);

        if (profile.isEmpty()) {
            return ApiResponse.error("Profile not found: " + name);
        }

        return ApiResponse.success(profile.get());
    }

    /**
     * Returns total profile count.
     */
    @GetMapping("/count")
    public ApiResponse<Long> getProfileCount() {
        return ApiResponse.success(profileRepo.count());
    }
}

