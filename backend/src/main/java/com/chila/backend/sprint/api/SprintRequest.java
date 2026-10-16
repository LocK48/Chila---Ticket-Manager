package com.chila.backend.sprint.api;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SprintRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2000) String goal,
        LocalDate startDate,
        LocalDate endDate) { }
