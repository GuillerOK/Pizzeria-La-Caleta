package com.lacaleta.model;

import jakarta.persistence.*;

@Entity
@Table(name = "insumos")
public class Insumo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Long version;
    
    @Column(nullable = false, unique = true, length = 50)
    private String codigo;
    
    @Column(nullable = false, length = 100)
    private String nombre;
    
    @Column(name = "stock_actual", nullable = false)
    private Double stockActual;
    
    @Column(name = "unidad_medida", length = 50)
    private String unidadMedida; // kg, Litros, unidades, etc.
    
    @Column(name = "stock_minimo")
    private Double stockMinimo;   // Para calcular si está CRÍTICO u ÓPTIMO

    public Insumo() {}

    public Insumo(Integer id, String codigo, String nombre, Double stockActual, String unidadMedida, Double stockMinimo) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.stockActual = stockActual;
        this.unidadMedida = unidadMedida;
        this.stockMinimo = stockMinimo;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Double getStockActual() { return stockActual; }
    public void setStockActual(Double stockActual) { this.stockActual = stockActual; }

    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }

    public Double getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Double stockMinimo) { this.stockMinimo = stockMinimo; }

    public boolean isCritico() {
        return this.stockActual != null && this.stockMinimo != null && this.stockActual <= this.stockMinimo;
    }
}
