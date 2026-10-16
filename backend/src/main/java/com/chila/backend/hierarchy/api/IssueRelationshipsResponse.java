package com.chila.backend.hierarchy.api;
import java.util.List;
public record IssueRelationshipsResponse(RelatedIssueSummary parent, List<RelatedIssueSummary> children,
        List<IssueLinkResponse> links) { }
