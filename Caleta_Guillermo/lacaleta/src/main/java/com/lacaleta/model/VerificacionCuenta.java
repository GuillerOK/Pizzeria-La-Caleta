package com.lacaleta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "verificaciones_cuenta")
public class VerificacionCuenta {
    @Id
    @Column(length = 100)
    private String email;

    @Column(name = "codigo_hash", nullable = false, length = 60)
    private String codigoHash;

    @Column(nullable = false)
    private Instant vencimiento;

    @Column(nullable = false)
    private int intentos;

    public VerificacionCuenta() { }

    public VerificacionCuenta(String email, String codigoHash, Instant vencimiento) {
        this.email = email;
        this.codigoHash = codigoHash;
        this.vencimiento = vencimiento;
    }

    public String getEmail() { return email; }
    public String getCodigoHash() { return codigoHash; }
    public Instant getVencimiento() { return vencimiento; }
    public int getIntentos() { return intentos; }
    public void setIntentos(int intentos) { this.intentos = intentos; }
}
