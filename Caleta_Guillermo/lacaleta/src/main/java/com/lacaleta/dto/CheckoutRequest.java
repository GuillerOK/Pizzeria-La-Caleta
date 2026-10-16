package com.lacaleta.dto;

import java.util.List;

public record CheckoutRequest(String email, List<Item> items) {
    public record Item(Integer productoId, Integer cantidad) { }
}
