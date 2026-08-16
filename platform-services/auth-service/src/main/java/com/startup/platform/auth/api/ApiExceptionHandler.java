package com.startup.platform.auth.api;

import com.startup.platform.auth.identity.EmailAlreadyRegisteredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException exception) {

        String detail = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Request validation failed");

        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid Parameter",
                detail,
                "INVALID_INPUT");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidInput(
            IllegalArgumentException exception) {

        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid Parameter",
                exception.getMessage(),
                "INVALID_INPUT");
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ProblemDetail handleDuplicateEmail() {

        return problem(
                HttpStatus.CONFLICT,
                "Email Already Registered",
                "The email address is already registered.",
                "EMAIL_ALREADY_REGISTERED");
    }

    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail,
            String code) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                detail);

        problem.setTitle(title);
        problem.setType(java.net.URI.create(
                "urn:startup:auth:error:" +
                        code.toLowerCase().replace('_', '-')));
        problem.setProperty("code", code);
        problem.setProperty(
                "timestamp",
                OffsetDateTime.now(ZoneOffset.UTC));

        return problem;
    }
}
