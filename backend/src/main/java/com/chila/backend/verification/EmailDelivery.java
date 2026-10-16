package com.chila.backend.verification;

public interface EmailDelivery {
    void sendVerificationCode(String email, String code, EmailVerificationPurpose purpose);
}
