package com.myciti.controller;

import com.myciti.dto.UpdateLocationRequest;
import com.myciti.dto.UserLocationDto;
import com.myciti.model.User;
import com.myciti.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/location")
public class UserLocationController {

    private final UserService userService;

    public UserLocationController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserLocationDto> getLocation(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userService.getCoarseLocation(user.getId()));
    }

    @PutMapping
    public ResponseEntity<UserLocationDto> updateLocation(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody UpdateLocationRequest req
    ) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userService.updateCoarseLocation(user.getId(), req));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteLocation(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();
        userService.deleteLocationData(user.getId());
        return ResponseEntity.noContent().build();
    }
}
