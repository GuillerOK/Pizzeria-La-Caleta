package com.lacaleta.model;

import java.time.LocalDateTime;

public class Usuario {

    private Integer id;
    private String nombre;
    private String email;
    private String password;
    private String telefono;
    private String rol = "CLIENTE"; // "CLIENTE" o "ADMIN"
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    public Usuario() {}

    public Usuario(Integer id, String nombre, String email, String password, String telefono, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.telefono = telefono;
        this.rol = rol;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}