package com.chila.backend.issue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record IssueKey(String projectKey, long issueNumber) {
    private static final Pattern FORMAT = Pattern.compile("^([A-Z][A-Z0-9]{1,9})-([1-9][0-9]*)$");

    public IssueKey {
        if (projectKey == null || issueNumber < 1) throw new IllegalArgumentException("Invalid issue key.");
    }

    public static IssueKey parse(String value) {
        if (value == null) throw new IllegalArgumentException("Invalid issue key.");
        Matcher matcher = FORMAT.matcher(value.trim().toUpperCase(java.util.Locale.ROOT));
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid issue key.");
        try {
            return new IssueKey(matcher.group(1), Long.parseLong(matcher.group(2)));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid issue key.", exception);
        }
    }

    @Override
    public String toString() {
        return projectKey + "-" + issueNumber;
    }
}
