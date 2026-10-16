package com.chila.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieService {

    static final String ACCESS_COOKIE = "CHILA_ACCESS";

    private final boolean secureCookies;
    private final String sameSite;

    public AuthCookieService(
            @Value("${chila.auth.cookie-secure}") boolean secureCookies,
            @Value("${chila.auth.cookie-same-site:Lax}") String sameSite) {
        this.secureCookies = secureCookies;
        this.sameSite = sameSite;
    }

    public ResponseCookie accessCookie(String value) {
        return ResponseCookie.from(ACCESS_COOKIE, value)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(sameSite)
                .path("/api")
                .maxAge(JwtTokenService.ACCESS_TOKEN_SECONDS)
                .build();
    }

    public ResponseCookie clearAccessCookie() {
        return ResponseCookie.from(ACCESS_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(sameSite)
                .path("/api")
                .maxAge(0)
                .build();
    }
}
