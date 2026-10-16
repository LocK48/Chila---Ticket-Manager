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
import com.chila.backend.verification.EmailVerificationPurpose;
import com.chila.backend.verification.EmailVerificationService;
import com.chila.backend.verification.PasswordChangeRequest;
import com.chila.backend.verification.VerificationResult;

@Service
public class UserService {

    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerification;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailVerificationService emailVerification) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerification = emailVerification;
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
        requireValidCode(emailVerification.verify(email, EmailVerificationPurpose.REGISTRATION, null, request.verificationCode()));

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

    @Transactional
    public void requestPasswordChangeCode(UUID userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        emailVerification.requestPasswordChangeCode(user);
    }

    @Transactional
    public void changePassword(UUID userId, PasswordChangeRequest request) {
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES)
            throw new InvalidInputException("newPassword", "Password must be no more than 72 UTF-8 bytes.");
        UserEntity user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        requireValidCode(emailVerification.verify(user.getEmail(), EmailVerificationPurpose.PASSWORD_CHANGE,
                user, request.verificationCode()));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private static void requireValidCode(VerificationResult result) {
        if (result != VerificationResult.VALID) {
            String message = result == VerificationResult.TOO_MANY_ATTEMPTS
                    ? "Too many incorrect verification attempts. Request a new code."
                    : "The verification code is invalid, expired, or already used.";
            throw new InvalidInputException("verificationCode", message);
        }
    }

    private UserResponse toResponse(UserEntity user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
    }
}
