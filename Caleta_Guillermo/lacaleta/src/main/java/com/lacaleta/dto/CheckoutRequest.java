package com.lacaleta.dto;

import java.util.List;

public record CheckoutRequest(String email, List<Item> items, String metodoPago,
                              String tipoEntrega, String direccionEntrega, String claveCompra,
                              String codigoCupon) {
    public CheckoutRequest(String email, List<Item> items, String metodoPago,
                           String tipoEntrega, String direccionEntrega, String claveCompra) {
        this(email, items, metodoPago, tipoEntrega, direccionEntrega, claveCompra, null);
    }
    public record Item(Integer productoId, Integer cantidad) { }
}
