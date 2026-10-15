package com.chila.backend.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {

    @Override
    public String resolve(HttpServletRequest request) {
        if ("/api/v1/auth/logout".equals(request.getRequestURI())) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String token = null;
        for (Cookie cookie : cookies) {
            if ("CHILA_ACCESS".equals(cookie.getName())) {
                if (token != null) {
                    throw new InvalidBearerTokenException(BearerTokenErrorCodes.INVALID_TOKEN);
                }
                token = cookie.getValue();
            }
        }
        return StringUtils.hasText(token) ? token : null;
    }
}
