package com.example.fooddelivery.order.api;

import com.example.fooddelivery.order.domain.OrderNotFoundException;
import com.example.fooddelivery.order.domain.Pricing.InvalidCartException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Domain exceptions → RFC 9457 problem details. Validation errors are handled by Spring's own advice. */
@RestControllerAdvice
public class OrderExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail notFound(OrderNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidCartException.class)
    ProblemDetail invalidCart(InvalidCartException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
        problem.setTitle("Invalid cart");
        return problem;
    }
}
