package com.chila.backend.verification;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import com.chila.backend.common.api.InvalidInputException;
import com.chila.backend.user.UserEntity;
import com.chila.backend.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {
    private static final Duration CODE_LIFETIME = Duration.ofMinutes(10);
    private static final Duration RESEND_DELAY = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;
    private final EmailVerificationCodeRepository codes;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final EmailDelivery emailDelivery;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(EmailVerificationCodeRepository codes, UserRepository users,
            PasswordEncoder passwordEncoder, EmailDelivery emailDelivery) {
        this.codes = codes; this.users = users; this.passwordEncoder = passwordEncoder; this.emailDelivery = emailDelivery;
    }

    @Transactional
    public void requestRegistrationCode(String rawEmail) {
        String email = normalize(rawEmail);
        // Keep this endpoint's response indistinguishable for existing and new addresses.
        if (users.existsByEmail(email)) return;
        issue(null, email, EmailVerificationPurpose.REGISTRATION);
    }

    @Transactional
    public void requestPasswordChangeCode(UserEntity user) {
        issue(user, user.getEmail(), EmailVerificationPurpose.PASSWORD_CHANGE);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VerificationResult verify(String rawEmail, EmailVerificationPurpose purpose, UserEntity user, String code) {
        String email = normalize(rawEmail);
        List<EmailVerificationCodeEntity> active = codes.lockActiveByEmailAndPurpose(email, purpose);
        if (active.isEmpty()) return VerificationResult.MISSING;
        EmailVerificationCodeEntity latest = active.getFirst();
        if (user == null ? latest.getUser() != null : latest.getUser() == null || !latest.getUser().getId().equals(user.getId()))
            return VerificationResult.MISSING;
        Instant now = Instant.now();
        if (latest.getAttempts() >= MAX_ATTEMPTS) return VerificationResult.TOO_MANY_ATTEMPTS;
        if (latest.getExpiresAt().isBefore(now)) {
            latest.consume(now);
            return VerificationResult.EXPIRED;
        }
        if (!passwordEncoder.matches(code, latest.getCodeHash())) {
            latest.incrementAttempts();
            return latest.getAttempts() >= MAX_ATTEMPTS ? VerificationResult.TOO_MANY_ATTEMPTS : VerificationResult.INVALID;
        }
        latest.consume(now);
        return VerificationResult.VALID;
    }

    private void issue(UserEntity user, String email, EmailVerificationPurpose purpose) {
        Instant now = Instant.now();
        codes.findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, purpose).ifPresent(previous -> {
            if (Duration.between(previous.getCreatedAt(), now).compareTo(RESEND_DELAY) < 0)
                throw new InvalidInputException("email", "Wait 60 seconds before requesting another verification code.");
        });
        codes.lockActiveByEmailAndPurpose(email, purpose).forEach(active -> active.consume(now));
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        codes.saveAndFlush(new EmailVerificationCodeEntity(user, email, purpose, passwordEncoder.encode(code), now.plus(CODE_LIFETIME)));
        emailDelivery.sendVerificationCode(email, code, purpose);
    }

    public static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
