package com.chila.backend.verification;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCodeEntity, UUID> {
    Optional<EmailVerificationCodeEntity> findFirstByEmailAndPurposeOrderByCreatedAtDesc(String email, EmailVerificationPurpose purpose);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from EmailVerificationCodeEntity c where c.email = :email and c.purpose = :purpose and c.consumedAt is null order by c.createdAt desc")
    java.util.List<EmailVerificationCodeEntity> lockActiveByEmailAndPurpose(@Param("email") String email,
            @Param("purpose") EmailVerificationPurpose purpose);
}
