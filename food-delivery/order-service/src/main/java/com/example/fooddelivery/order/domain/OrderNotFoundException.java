package com.example.fooddelivery.order.domain;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(long id) {
        super("order " + id + " not found");
    }
}
