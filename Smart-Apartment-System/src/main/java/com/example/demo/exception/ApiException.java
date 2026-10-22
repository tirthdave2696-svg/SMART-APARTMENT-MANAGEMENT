package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import java.util.List;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final List<String> errors;

    public ApiException(HttpStatus status, String message) { this(status, message, List.of()); }
    public ApiException(HttpStatus status, String message, List<String> errors) {
        super(message); this.status = status; this.errors = errors;
    }
    public HttpStatus getStatus() { return status; }
    public List<String> getErrors() { return errors; }
}