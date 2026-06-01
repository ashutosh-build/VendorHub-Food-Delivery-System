package com.vendorhub.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, Object>> build(int status, String message, Object errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success",   false);
        body.put("status",    status);
        body.put("message",   message);
        body.put("timestamp", LocalDateTime.now().toString());
        if (errors != null) body.put("errors", errors);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handle(ApiException ex) {
        return build(ex.getStatus().value(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handle(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errs = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> Map.of("field", fe.getField(),
                              "message", Objects.requireNonNullElse(fe.getDefaultMessage(), "Invalid")))
            .toList();
        return build(422, "Validation failed", errs);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handle(AccessDeniedException ex) {
        return build(403, "Access denied", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handle(Exception ex) {
        return build(500, "Server error: " + ex.getMessage(), null);
    }
}
