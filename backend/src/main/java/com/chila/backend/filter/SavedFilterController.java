package com.chila.backend.filter;

import java.util.List;
import java.util.UUID;
import com.chila.backend.filter.api.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class SavedFilterController {
    private final SavedFilterService service;
    public SavedFilterController(SavedFilterService service){this.service=service;}
    @GetMapping("/api/v1/projects/{projectId}/filters") public List<SavedFilterResponse> list(JwtAuthenticationToken auth,@PathVariable UUID projectId){return service.list(userId(auth),projectId);}
    @PostMapping("/api/v1/projects/{projectId}/filters") @ResponseStatus(HttpStatus.CREATED) public SavedFilterResponse create(JwtAuthenticationToken auth,@PathVariable UUID projectId,@Valid @RequestBody SavedFilterRequest request){return service.create(userId(auth),projectId,request);}
    @PutMapping("/api/v1/projects/{projectId}/filters/{filterId}") public SavedFilterResponse update(JwtAuthenticationToken auth,@PathVariable UUID projectId,@PathVariable UUID filterId,@Valid @RequestBody SavedFilterRequest request){return service.update(userId(auth),projectId,filterId,request);}
    @DeleteMapping("/api/v1/projects/{projectId}/filters/{filterId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(JwtAuthenticationToken auth,@PathVariable UUID projectId,@PathVariable UUID filterId){service.delete(userId(auth),projectId,filterId);}
    private static UUID userId(JwtAuthenticationToken auth){return UUID.fromString(auth.getToken().getSubject());}
}
