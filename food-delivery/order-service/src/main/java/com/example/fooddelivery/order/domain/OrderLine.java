package com.example.fooddelivery.order.domain;

public record OrderLine(Long menuItemId, String name, int quantity, long unitPricePaise) {

    public long lineTotalPaise() {
        return Math.multiplyExact(unitPricePaise, quantity);
    }
}
