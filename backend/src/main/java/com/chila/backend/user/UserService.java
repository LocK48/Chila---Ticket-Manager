package com.chila.backend.user;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import com.chila.backend.common.api.InvalidInputException;
import com.chila.backend.user.api.RegisterRequest;
import com.chila.backend.user.api.UserResponse;
import com.chila.backend.user.api.UpdateProfileRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String displayName = request.displayName().trim();

        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new InvalidInputException("password", "Password must be no more than 72 UTF-8 bytes.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        UserEntity user = new UserEntity(email, passwordEncoder.encode(request.password()), displayName);
        try {
            return toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            // The unique constraint handles concurrent requests that pass the pre-check together.
            throw new DuplicateEmailException();
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        return toResponse(userRepository.findById(userId).orElseThrow(UserNotFoundException::new));
    }

    @Transactional
    public UserResponse updateCurrentUser(UUID userId, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.setDisplayName(request.displayName().trim());
        return toResponse(user);
    }

    private UserResponse toResponse(UserEntity user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
    }
}
