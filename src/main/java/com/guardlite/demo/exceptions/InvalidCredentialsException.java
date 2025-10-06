package com.guardlite.demo.exceptions;

public class InvalidCredentialsException extends org.springframework.security.core.AuthenticationException {
    public InvalidCredentialsException() {
        super("invalid_credentials");
    }
}