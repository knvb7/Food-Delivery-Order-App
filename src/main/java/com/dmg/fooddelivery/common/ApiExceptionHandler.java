package com.dmg.fooddelivery.common;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> domain(ApiException exception) {
        return ResponseEntity.status(exception.status())
                .body(
                        ApiError.of(
                                exception.status().value(),
                                exception.code(),
                                exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            validationErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ApiError error =
                new ApiError(
                        Instant.now(),
                        400,
                        "VALIDATION_FAILED",
                        "Input validation failed",
                        validationErrors);

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingRequestHeaderException.class,
        MissingServletRequestParameterException.class,
        ConstraintViolationException.class,
        HandlerMethodValidationException.class
    })
    ResponseEntity<ApiError> malformed(Exception exception) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVALID_REQUEST", "Invalid or missing request data"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException exception) {
        return ResponseEntity.status(409)
                .body(ApiError.of(409, "CONFLICT", "This change conflicts with existing data"));
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    ResponseEntity<ApiError> concurrency(ConcurrencyFailureException exception) {
        return ResponseEntity.status(409)
                .body(ApiError.of(409, "CONCURRENT_UPDATE", "Resource is busy; retry the request"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> missing(NoResourceFoundException exception) {
        return ResponseEntity.status(404).body(ApiError.of(404, "NOT_FOUND", "Endpoint not found"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> method(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(405)
                .body(ApiError.of(405, "METHOD_NOT_ALLOWED", "HTTP method not supported"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> mediaType(HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(415)
                .body(
                        ApiError.of(
                                415,
                                "UNSUPPORTED_MEDIA_TYPE",
                                "Use Content-Type: application/json"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception) {
        log.error("Unhandled request failure", exception);

        return ResponseEntity.internalServerError()
                .body(ApiError.of(500, "INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
