package com.chila.backend.user;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("User account was not found.");
    }
}
