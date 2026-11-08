package com.soham.taskmanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Global exception handler that catches exceptions thrown by controllers
 * and returns consistent, structured JSON error responses.
 *
 * <p>Every error body contains {@code timestamp}, {@code status} and {@code error};
 * most also carry a human-readable {@code message}.</p>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle resource not found (404).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    /**
     * Handle validation errors (400) — returns field-level error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, "Validation Failed");
        body.put("fieldErrors", fieldErrors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle illegal argument errors (400).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    /**
     * Catch-all handler for unexpected errors (500).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred");
    }

    // --- Helpers ---

    private static Map<String, Object> baseBody(HttpStatus status, String error) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        return body;
    }

    private static ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String error, String message) {
        Map<String, Object> body = baseBody(status, error);
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}
