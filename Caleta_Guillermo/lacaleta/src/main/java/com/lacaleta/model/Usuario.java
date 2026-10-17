package com.lacaleta.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // Mapea exactamente la columna 'password_hash' de PostgreSQL
    @Column(name = "password_hash", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;

    @Column(length = 15)
    private String telefono;

    @Column(length = 20)
    private String dni;

    // Mapea exactamente la columna 'rol_id' de PostgreSQL (Integer).
    // 2 = ADMIN, 1 = CLIENTE segun tabla 'roles'
    @Column(name = "rol_id", nullable = false)
    private Integer rolId;

    @Column(name = "puntos")
    private Integer puntos = 50;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @Column(name = "verificado", columnDefinition = "boolean default false")
    private Boolean verificado = false;

    public Usuario() {
        this.rolId = 1; // CLIENTE por defecto
        this.fechaRegistro = LocalDateTime.now();
    }

    public Usuario(Integer id, String nombre, String email, String password, String telefono, String dni, Integer rolId) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.telefono = telefono;
        this.dni = dni;
        this.rolId = (rolId != null) ? rolId : 1;
        this.fechaRegistro = LocalDateTime.now();
    }

    public Integer getPuntos() {
        return puntos != null ? puntos : 0;
    }

    public void setPuntos(Integer puntos) {
        this.puntos = puntos;
    }

    public Boolean getVerificado() {
        return verificado;
    }

    public void setVerificado(Boolean verificado) {
        this.verificado = verificado;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public Integer getRolId() {
        return rolId;
    }

    public void setRolId(Integer rolId) {
        this.rolId = rolId;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    /**
     * Helper: devuelve true si el usuario es ADMIN (rolId == 2).
     * Usar en controladores en lugar de comparar Strings.
     */
    public boolean esAdmin() {
        return Integer.valueOf(2).equals(this.rolId);
    }
}
