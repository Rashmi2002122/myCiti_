package com.myciti.controller;

import com.myciti.dto.UpdateProfileRequest;
import com.myciti.dto.UserDto;
import com.myciti.model.User;
import com.myciti.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/me
     * Returns current user's profile, role, profileComplete.
     */
    @GetMapping
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        UserDto profile = userService.getProfile(currentUser.getId());
        return ResponseEntity.ok(profile);
    }

    /**
     * PUT /api/me
     * Completes/updates profile: accountType, businessName, phone.
     */
    @PutMapping
    public ResponseEntity<UserDto> updateProfile(
        @AuthenticationPrincipal User currentUser,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        UserDto updated = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(updated);
    }
}
