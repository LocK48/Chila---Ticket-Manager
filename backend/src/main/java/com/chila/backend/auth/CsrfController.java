package com.chila.backend.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class CsrfController {

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrfBootstrap() {
        // Spring Security's SPA CSRF handler creates the XSRF-TOKEN cookie before this runs.
        return ResponseEntity.noContent().build();
    }
}
