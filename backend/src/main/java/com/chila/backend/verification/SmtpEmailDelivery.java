package com.chila.backend.verification;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SmtpEmailDelivery implements EmailDelivery {
    private final ObjectProvider<JavaMailSender> senders;
    private final Environment environment;
    private final String configuredFrom;

    public SmtpEmailDelivery(ObjectProvider<JavaMailSender> senders, Environment environment,
            @Value("${chila.email.from:}") String configuredFrom) {
        this.senders = senders; this.environment = environment; this.configuredFrom = configuredFrom;
    }

    @Override
    public void sendVerificationCode(String email, String code, EmailVerificationPurpose purpose) {
        String host = environment.getProperty("spring.mail.host", "");
        JavaMailSender sender = senders.getIfAvailable();
        String from = StringUtils.hasText(configuredFrom) ? configuredFrom : environment.getProperty("spring.mail.username", "");
        if (!StringUtils.hasText(host) || sender == null || !StringUtils.hasText(from))
            throw new EmailDeliveryUnavailableException();
        String purposeLabel = purpose == EmailVerificationPurpose.REGISTRATION ? "complete your registration" : "change your password";
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(email); message.setSubject("Your Chila verification code");
        message.setText("Use this code to " + purposeLabel + ": " + code + "\n\n"
                + "The code expires in 10 minutes. If you did not request it, you can ignore this email.");
        try { sender.send(message); }
        catch (RuntimeException exception) { throw new EmailDeliveryUnavailableException(); }
    }
}
