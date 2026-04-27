package com.powermobile.challenge.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", "Erro de validação");

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            if (error instanceof FieldError fieldError) {
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                errors.put(error.getObjectName(), error.getDefaultMessage());
            }
        });

        response.put("errors", errors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());

        String message = ex.getMessage() != null ? ex.getMessage() : "Erro inesperado";

        if (message.contains("not found")) {
            response.put("status", HttpStatus.NOT_FOUND.value());
            response.put("message", message);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        if (message.contains("Unauthorized") || message.contains("Not your turn")) {
            response.put("status", HttpStatus.FORBIDDEN.value());
            response.put("message", message);
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }

        if (message.contains("already") || message.contains("already exists")) {
            response.put("status", HttpStatus.CONFLICT.value());
            response.put("message", message);
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", message);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());

        String message = "Data integrity error";
        int statusCode = HttpStatus.CONFLICT.value();
        String exceptionMessage = ex.getMessage() != null ? ex.getMessage() : "";

        if (exceptionMessage.contains("doesn't have a default value")) {
            message = "Internal server error — database field not configured correctly.";
            statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        } else if (exceptionMessage.contains("Duplicate entry") && exceptionMessage.contains("clientEmail")) {
            message = "Email already registered in the system. A client with this email already exists.";
        } else if (exceptionMessage.contains("Duplicate entry") && (exceptionMessage.contains("proposal_id") || exceptionMessage.contains("proposalId"))) {
            message = "A contract already exists for this proposal.";
        } else if (exceptionMessage.contains("Duplicate entry")) {
            message = "Duplicate record — this value already exists in the system.";
        }

        response.put("status", statusCode);
        response.put("message", message);
        return new ResponseEntity<>(response, HttpStatus.valueOf(statusCode));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("message", "Erro interno do servidor");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
