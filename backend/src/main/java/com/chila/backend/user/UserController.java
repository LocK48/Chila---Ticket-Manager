package com.chila.backend.user;

import java.util.UUID;

import com.chila.backend.user.api.UpdateProfileRequest;
import com.chila.backend.user.api.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse getCurrentUser(JwtAuthenticationToken authentication) {
        return userService.getCurrentUser(userId(authentication));
    }

    @PatchMapping
    public UserResponse updateCurrentUser(
            JwtAuthenticationToken authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateCurrentUser(userId(authentication), request);
    }

    private UUID userId(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getToken().getSubject());
    }
}
