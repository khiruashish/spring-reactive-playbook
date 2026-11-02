package com.example.reactive.playbook.web;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String id) {
        super("product " + id + " not found");
    }
}
