package com.fashionhub.fulfillment.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

/** Consistent RFC-7807 problem responses; never leaks stack traces. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errors.put(f.getField(), f.getDefaultMessage()));
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler({BadRequestException.class, HttpMessageNotReadableException.class})
    public ProblemDetail badRequest(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail notFound(ResourceNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail conflict(ConflictException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(EngineUnavailableException.class)
    public ProblemDetail engine(EngineUnavailableException e) {
        log.warn("Engine problem: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail other(Exception e) {
        log.error("Unhandled error", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }
}
