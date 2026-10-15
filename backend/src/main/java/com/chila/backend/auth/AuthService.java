package com.chila.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.chila.backend.auth.api.LoginRequest;
import com.chila.backend.user.UserEntity;
import com.chila.backend.user.UserRepository;
import com.chila.backend.user.api.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidCredentialsException();
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        UserEntity user = userRepository.findByEmail(email)
                .filter(existing -> passwordEncoder.matches(request.password(), existing.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return new LoginResult(toResponse(user), jwtTokenService.createAccessToken(user));
    }

    private UserResponse toResponse(UserEntity user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
    }

    public record LoginResult(UserResponse user, String accessToken) {
    }
}
