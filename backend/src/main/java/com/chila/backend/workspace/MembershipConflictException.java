package com.chila.backend.workspace;

public class MembershipConflictException extends RuntimeException {
    public MembershipConflictException() {
        super("The user is already a member of this workspace.");
    }
}
