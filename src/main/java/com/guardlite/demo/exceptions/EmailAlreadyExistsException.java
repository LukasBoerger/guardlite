package com.guardlite.demo.exceptions;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("email_exists:" + email);
    }
}