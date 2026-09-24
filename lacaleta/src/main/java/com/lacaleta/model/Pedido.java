package com.lacaleta.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Pedido {

    private Integer id;
    private Usuario usuario;
    private LocalDateTime fecha = LocalDateTime.now();
    private String estado = "PENDIENTE"; // PENDIENTE, EN_CAMINO, ENTREGADO
    private String metodoPago; // YAPE, TARJETA, EFECTIVO
    private String direccionEntrega;
    private Double total;
    private List<DetallePedido> detalles = new ArrayList<>();

    public Pedido() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public List<DetallePedido> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePedido> detalles) { this.detalles = detalles; }
}