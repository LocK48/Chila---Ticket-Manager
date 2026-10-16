package com.chila.backend.hierarchy.api;
import jakarta.validation.constraints.*;
public record EpicRequest(@NotBlank @Size(max = 120) String name, @Size(max = 2000) String summary,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color) { }
