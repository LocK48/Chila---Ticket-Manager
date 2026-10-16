package com.chila.backend.hierarchy.api;
import java.time.LocalDate;
import com.chila.backend.hierarchy.ReleaseState;
import jakarta.validation.constraints.*;
public record ReleaseRequest(@NotBlank @Size(max = 120) String name, @Size(max = 2000) String description,
        LocalDate startDate, LocalDate releaseDate, ReleaseState state) { }
