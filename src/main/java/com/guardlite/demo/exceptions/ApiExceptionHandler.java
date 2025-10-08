package com.guardlite.demo.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler({InvalidCredentialsException.class,
            BadCredentialsException.class,
            UsernameNotFoundException.class})
    public ResponseEntity<ApiError> handleAuth(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(HttpStatus.UNAUTHORIZED, "invalid_credentials",
                        "E-Mail oder Passwort ist falsch.", req));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleEmail(EmailAlreadyExistsException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(HttpStatus.CONFLICT, "email_exists",
                        "Für diese E-Mail existiert bereits ein Account.", req));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(HttpStatus.BAD_REQUEST, "invalid_request", "Request ungültig.", req));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex, HttpServletRequest req) {
        log.error("Unhandled error on {} {}",
                req.getMethod(), req.getRequestURI(), ex);  // <- Stacktrace ins Log
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Unerwarteter Fehler.", req));
    }

    record ApiError(Instant timestamp, int status, String code, String message, String path) {
        static ApiError of(HttpStatus s, String code, String msg, HttpServletRequest req) {
            return new ApiError(Instant.now(), s.value(), code, msg, req.getRequestURI());
        }
    }
}