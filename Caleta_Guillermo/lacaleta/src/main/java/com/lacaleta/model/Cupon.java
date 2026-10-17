package com.lacaleta.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cupones")
public class Cupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false, length = 30)
    private String tipo; // PORCENTAJE, MONTO_FIJO, DELIVERY_GRATIS, REGALO

    @Column(nullable = false)
    private Double descuento;

    @Column(name = "puntos_requeridos", nullable = false)
    private Integer puntosRequeridos;

    @Column(nullable = false, length = 20)
    private String estado = "ACTIVO"; // ACTIVO, USADO, EXPIRADO

    @Column(name = "fecha_canje", nullable = false)
    private LocalDateTime fechaCanje = LocalDateTime.now();

    @Column(name = "fecha_vencimiento")
    private LocalDateTime fechaVencimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    public Cupon() {}

    public Cupon(String codigo, String titulo, String descripcion, String tipo, Double descuento, Integer puntosRequeridos, Usuario usuario, LocalDateTime fechaVencimiento) {
        this.codigo = codigo;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.descuento = descuento;
        this.puntosRequeridos = puntosRequeridos;
        this.usuario = usuario;
        this.estado = "ACTIVO";
        this.fechaCanje = LocalDateTime.now();
        this.fechaVencimiento = fechaVencimiento != null ? fechaVencimiento : LocalDateTime.now().plusDays(30);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Double getDescuento() {
        return descuento;
    }

    public void setDescuento(Double descuento) {
        this.descuento = descuento;
    }

    public Integer getPuntosRequeridos() {
        return puntosRequeridos;
    }

    public void setPuntosRequeridos(Integer puntosRequeridos) {
        this.puntosRequeridos = puntosRequeridos;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCanje() {
        return fechaCanje;
    }

    public void setFechaCanje(LocalDateTime fechaCanje) {
        this.fechaCanje = fechaCanje;
    }

    public LocalDateTime getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDateTime fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
