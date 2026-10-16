package com.chila.backend.hierarchy.api;
import java.util.UUID;
import com.chila.backend.hierarchy.IssueLinkType;
import jakarta.validation.constraints.NotNull;
public record IssueLinkRequest(@NotNull UUID targetIssueId, @NotNull IssueLinkType type) { }
