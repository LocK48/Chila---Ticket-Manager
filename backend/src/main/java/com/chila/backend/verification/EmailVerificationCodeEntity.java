package com.chila.backend.verification;

import java.time.Instant;
import java.util.UUID;
import com.chila.backend.user.UserEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "email_verification_codes", indexes = @Index(name = "ix_email_verification_email_purpose_created", columnList = "email, purpose, created_at"))
public class EmailVerificationCodeEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private UserEntity user;
    @Column(nullable = false, length = 320) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private EmailVerificationPurpose purpose;
    @Column(name = "code_hash", nullable = false, length = 100) private String codeHash;
    @Column(nullable = false) private short attempts;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "consumed_at") private Instant consumedAt;

    protected EmailVerificationCodeEntity() { }
    public EmailVerificationCodeEntity(UserEntity user, String email, EmailVerificationPurpose purpose, String codeHash, Instant expiresAt) {
        this.user = user; this.email = email; this.purpose = purpose; this.codeHash = codeHash; this.expiresAt = expiresAt;
    }
    public String getCodeHash() { return codeHash; }
    public UserEntity getUser() { return user; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public short getAttempts() { return attempts; }
    public void incrementAttempts() { attempts++; }
    public void consume(Instant now) { consumedAt = now; }
    public boolean isConsumed() { return consumedAt != null; }
}
