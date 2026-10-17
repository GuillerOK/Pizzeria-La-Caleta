package com.lacaleta.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Outbox: se confirma atómicamente junto con el pedido y sobrevive a reinicios. */
@Entity
@Table(name = "boletas_pendientes", indexes = @Index(name = "idx_boletas_pendientes_fecha", columnList = "proximo_intento"))
public class BoletaPendiente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Integer pedidoId;
    @Column(nullable = false)
    private String destinatario;
    @Column(nullable = false, columnDefinition = "text")
    private String html;
    @Column(nullable = false, name = "proximo_intento")
    private Instant proximoIntento = Instant.now();
    private int intentos;
    public BoletaPendiente() { }
    public BoletaPendiente(Integer pedidoId, String destinatario, String html) {
        this.pedidoId = pedidoId; this.destinatario = destinatario; this.html = html;
    }
    public Long getId() { return id; }
    public Integer getPedidoId() { return pedidoId; }
    public String getDestinatario() { return destinatario; }
    public String getHtml() { return html; }
    public int getIntentos() { return intentos; }
    public void reintentar() {
        intentos++;
        proximoIntento = Instant.now().plusSeconds(Math.min(3600, 30L * intentos));
    }
}
