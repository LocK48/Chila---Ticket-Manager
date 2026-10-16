package com.chila.backend.sprint.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record SprintIssueSelectionRequest(@NotEmpty @Size(max = 100) List<UUID> issueIds) { }
