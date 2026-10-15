package com.chila.backend.auth;

import com.chila.backend.auth.api.LoginRequest;
import com.chila.backend.user.api.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final CookieCsrfTokenRepository csrfTokenRepository;

    public AuthController(
            AuthService authService,
            AuthCookieService authCookieService,
            CookieCsrfTokenRepository csrfTokenRepository) {
        this.authService = authService;
        this.authCookieService = authCookieService;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        AuthService.LoginResult result = authService.login(request);
        csrfTokenRepository.saveToken(null, servletRequest, servletResponse);
        servletResponse.addHeader(HttpHeaders.SET_COOKIE,
                authCookieService.accessCookie(result.accessToken()).toString());
        return ResponseEntity.ok(result.user());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        csrfTokenRepository.saveToken(null, servletRequest, servletResponse);
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, authCookieService.clearAccessCookie().toString());
        return ResponseEntity.noContent().build();
    }
}
