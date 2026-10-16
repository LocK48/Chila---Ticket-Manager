package com.chila.backend.sprint.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.chila.backend.sprint.SprintState;

public record SprintResponse(UUID id, UUID projectId, String name, String goal, SprintState state,
        LocalDate startDate, LocalDate endDate, Instant createdAt, Instant updatedAt) { }
