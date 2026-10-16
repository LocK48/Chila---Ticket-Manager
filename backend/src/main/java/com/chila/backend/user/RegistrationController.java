package com.chila.backend.user;

import com.chila.backend.user.api.RegisterRequest;
import com.chila.backend.user.api.UserResponse;
import com.chila.backend.verification.EmailOtpRequest;
import com.chila.backend.verification.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class RegistrationController {

    private final UserService userService;
    private final EmailVerificationService emailVerification;

    public RegistrationController(UserService userService, EmailVerificationService emailVerification) {
        this.userService = userService;
        this.emailVerification = emailVerification;
    }

    @PostMapping("/registration-otp")
    public ResponseEntity<Void> requestRegistrationCode(@Valid @RequestBody EmailOtpRequest request) {
        emailVerification.requestRegistrationCode(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }
}
