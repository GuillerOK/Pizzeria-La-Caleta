package com.lacaleta.model;

public class Producto {
    
    private Integer id;
    private Categoria categoria;
    private String nombre;
    private String descripcion;
    private Double precio;
    private String imagenUrl;
    private Boolean disponible = true;

    public Producto() {}

    public Producto(Integer id, Categoria categoria, String nombre, String descripcion, Double precio, String imagenUrl, Boolean disponible) {
        this.id = id;
        this.categoria = categoria;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.imagenUrl = imagenUrl;
        this.disponible = disponible;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public Boolean getDisponible() { return disponible; }
    public void setDisponible(Boolean disponible) { this.disponible = disponible; }
}