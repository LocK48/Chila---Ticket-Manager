package com.chila.backend.verification;

public class EmailDeliveryUnavailableException extends RuntimeException {
    public EmailDeliveryUnavailableException() { super("Email delivery is unavailable. Configure SMTP to send verification codes."); }
}
