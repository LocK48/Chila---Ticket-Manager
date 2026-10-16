package com.chila.backend.verification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(@NotBlank @Pattern(regexp = "[0-9]{6}") String verificationCode,
        @NotBlank @Size(min = 12, max = 72) String newPassword) { }
