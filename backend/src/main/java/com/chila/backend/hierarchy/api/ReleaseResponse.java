package com.chila.backend.hierarchy.api;
import java.time.*;
import java.util.UUID;
import com.chila.backend.hierarchy.ReleaseState;
public record ReleaseResponse(UUID id, UUID projectId, String name, String description, LocalDate startDate,
        LocalDate releaseDate, ReleaseState state, long issueCount, Instant createdAt, Instant updatedAt) { }
