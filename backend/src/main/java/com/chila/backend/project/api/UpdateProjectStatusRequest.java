package com.chila.backend.project.api;

import com.chila.backend.project.ProjectStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateProjectStatusRequest(@NotNull ProjectStatus status) {
}
