package com.maze.nlpcustomerffeedbackanalyzer;

import com.maze.nlpcustomerffeedbackanalyzer.feedback.FeedbackDTOs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<FeedbackDTOs.ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(FeedbackDTOs.ErrorResponse.builder()
                .message("Validation failed")
                .details(details)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<FeedbackDTOs.ErrorResponse> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(FeedbackDTOs.ErrorResponse.builder()
                .message("Resource not found")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<FeedbackDTOs.ErrorResponse> handleRuntime(RuntimeException ex) {
        log.error("Unhandled runtime exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(FeedbackDTOs.ErrorResponse.builder()
                .message("Internal server error")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
