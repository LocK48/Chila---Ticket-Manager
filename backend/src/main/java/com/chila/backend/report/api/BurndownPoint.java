package com.chila.backend.report.api;

import java.time.LocalDate;

public record BurndownPoint(LocalDate date, long scopeIssues, long remainingIssues) { }
