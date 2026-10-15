package com.chila.backend.issue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class IssueKeyTests {

    @Test
    void parsesKeysCaseInsensitivelyAndFormatsCanonically() {
        IssueKey key = IssueKey.parse("iss-42");

        assertEquals("ISS", key.projectKey());
        assertEquals(42, key.issueNumber());
        assertEquals("ISS-42", key.toString());
    }

    @Test
    void rejectsMalformedAndOverflowingKeys() {
        assertThrows(IllegalArgumentException.class, () -> IssueKey.parse("ISS-0"));
        assertThrows(IllegalArgumentException.class, () -> IssueKey.parse("ISS-01"));
        assertThrows(IllegalArgumentException.class, () -> IssueKey.parse("I-1"));
        assertThrows(IllegalArgumentException.class, () -> IssueKey.parse("ISS-999999999999999999999"));
    }
}
