package com.endfielders.erl.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("status", "VALIDATION_ERROR");
        body.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        body.put("errors", fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        
        System.err.println("[ERL CRITICAL ERROR] Something broke:");
        ex.printStackTrace();

        Map<String, Object> body = new HashMap<>();
        body.put("status", "ERROR");
        body.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        body.put("message", "Unexpected server error: " + ex.getMessage()); 

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}