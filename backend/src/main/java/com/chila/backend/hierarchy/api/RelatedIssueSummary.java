package com.chila.backend.hierarchy.api;
import java.util.UUID;
public record RelatedIssueSummary(UUID id, String issueKey, String title, String status) { }
