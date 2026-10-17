package com.lacaleta.model;

import jakarta.persistence.*;

/** Cantidad de insumo por unidad vendida, en la unidad de medida del insumo. */
@Entity
@Table(name = "receta_insumos", uniqueConstraints = @UniqueConstraint(columnNames = {"producto_id", "insumo_id"}))
public class RecetaInsumo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "producto_id")
    private Producto producto;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "insumo_id")
    private Insumo insumo;
    @Column(nullable = false, precision = 19, scale = 6)
    private java.math.BigDecimal cantidad;
    public Integer getId() { return id; }
    public Producto getProducto() { return producto; }
    public Insumo getInsumo() { return insumo; }
    public java.math.BigDecimal getCantidad() { return cantidad; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public void setInsumo(Insumo insumo) { this.insumo = insumo; }
    public void setCantidad(java.math.BigDecimal cantidad) { this.cantidad = cantidad; }
}
