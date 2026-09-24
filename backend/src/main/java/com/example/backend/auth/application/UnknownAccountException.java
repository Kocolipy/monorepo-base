package com.example.backend.auth.application;

/** Thrown when an administrative action names an account that does not exist. */
public class UnknownAccountException extends RuntimeException {

    public UnknownAccountException(String username) {
        super("No account named " + username);
    }
}
