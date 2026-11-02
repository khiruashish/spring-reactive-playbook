package com.example.reactive.playbook.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

// --8<-- [start:advice]
/** RFC 9457 problem details for every annotated controller. */
@RestControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    ProblemDetail notFound(ProductNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Product not found");
        return problem;
    }

    /** Thrown by @Valid on reactive bodies. */
    @ExceptionHandler(WebExchangeBindException.class)
    ProblemDetail invalid(WebExchangeBindException e) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        problem.setProperty("errors", e.getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .sorted()
                .toList());
        return problem;
    }
}
// --8<-- [end:advice]
