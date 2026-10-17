package com.lacaleta.service;

import com.lacaleta.model.*;
import com.lacaleta.repository.BoletaPendienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;

@Service
public class BoletaOutboxService {
    @org.springframework.beans.factory.annotation.Value("${app.boletas.envio-habilitado:true}")
    private boolean envioHabilitado = true;
    private final BoletaPendienteRepository pendientes;
    private final EmailService correo;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BoletaOutboxService.class);
    public BoletaOutboxService(BoletaPendienteRepository pendientes, EmailService correo) {
        this.pendientes = pendientes; this.correo = correo;
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void encolar(Pedido pedido, BigDecimal subtotal, BigDecimal envio) {
        pendientes.save(new BoletaPendiente(pedido.getId(), pedido.getUsuario().getEmail(),
                correo.renderizarBoleta(pedido, subtotal, envio)));
    }
    @Scheduled(fixedDelayString = "${app.boletas.intervalo-ms:10000}")
    @Transactional
    public void enviarPendientes() {
        if (!envioHabilitado) return;
        // Solo son visibles las filas de pedidos confirmados; SKIP LOCKED permite varios servidores.
        for (BoletaPendiente boleta : pendientes.bloquearPendientes()) {
            try {
                correo.enviarHtml(boleta.getDestinatario(), boleta.getPedidoId(), boleta.getHtml());
                pendientes.delete(boleta);
            } catch (CorreoException error) {
                boleta.reintentar();
                log.warn("Boleta pendiente pedido={}, intento={}", boleta.getPedidoId(), boleta.getIntentos());
            }
        }
    }
}
