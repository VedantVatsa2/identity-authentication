package com.startup.platform.auth.identity;

public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("Authentication failed");
    }
}