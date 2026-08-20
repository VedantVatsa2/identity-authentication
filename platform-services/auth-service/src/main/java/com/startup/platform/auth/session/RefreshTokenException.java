package com.startup.platform.auth.session;

public class RefreshTokenException extends RuntimeException {

    public RefreshTokenException() {
        super("Invalid refresh token.");
    }
}