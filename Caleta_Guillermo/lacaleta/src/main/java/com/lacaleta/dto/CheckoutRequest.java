package com.lacaleta.dto;

import java.util.List;

public record CheckoutRequest(String email, List<Item> items, String metodoPago,
                              String tipoEntrega, String direccionEntrega, String claveCompra) {
    public record Item(Integer productoId, Integer cantidad) { }
}
