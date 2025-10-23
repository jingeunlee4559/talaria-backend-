package com.example.talaria.backend.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(long id) {
        super("User not found: id=" + id);
    }
}
