package com.lacaleta.service;

import com.lacaleta.model.Usuario;
import com.lacaleta.model.VerificacionCuenta;
import com.lacaleta.repository.UsuarioRepository;
import com.lacaleta.repository.VerificacionCuentaRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
public class VerificacionCuentaService {
    public enum Resultado { VERIFICADA, INVALIDO, AGOTADO, NO_ENCONTRADO }

    private static final Duration VIGENCIA = Duration.ofMinutes(10);
    private static final int MAX_INTENTOS = 5;
    private final SecureRandom random = new SecureRandom();
    private final UsuarioRepository usuarios;
    private final VerificacionCuentaRepository verificaciones;
    private final EmailService emails;

    public VerificacionCuentaService(UsuarioRepository usuarios,
                                    VerificacionCuentaRepository verificaciones,
                                    EmailService emails) {
        this.usuarios = usuarios;
        this.verificaciones = verificaciones;
        this.emails = emails;
    }

    @Transactional
    public boolean enviarCodigo(String email) {
        String destino = email.trim().toLowerCase(Locale.ROOT);
        // El bloqueo en BD serializa envíos y verificaciones incluso con varios servidores.
        Optional<Usuario> usuario = usuarios.buscarPorEmailParaVerificacion(destino);
        if (usuario.isEmpty() || Boolean.TRUE.equals(usuario.get().getVerificado())) {
            return false;
        }
        String codigo = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        VerificacionCuenta verificacion = new VerificacionCuenta(destino,
                BCrypt.hashpw(codigo, BCrypt.gensalt()), Instant.now().plus(VIGENCIA));
        verificaciones.saveAndFlush(verificacion);
        if (!emails.enviarCodigoVerificacion(destino, codigo)) {
            verificaciones.deleteById(destino);
            return false;
        }
        return true;
    }

    @Transactional
    public Resultado verificar(String email, String codigo) {
        if (email == null || codigo == null || !codigo.matches("[0-9]{6}")) {
            return Resultado.INVALIDO;
        }
        String destino = email.trim().toLowerCase(Locale.ROOT);
        Optional<Usuario> usuarioOpt = usuarios.buscarPorEmailParaVerificacion(destino);
        if (usuarioOpt.isEmpty()) return Resultado.NO_ENCONTRADO;
        Optional<VerificacionCuenta> pendiente = verificaciones.findById(destino);
        if (pendiente.isEmpty()) return Resultado.INVALIDO;
        VerificacionCuenta verificacion = pendiente.get();
        if (Boolean.TRUE.equals(usuarioOpt.get().getVerificado())
                || !verificacion.getVencimiento().isAfter(Instant.now())) {
            verificaciones.deleteById(destino);
            return Resultado.INVALIDO;
        }
        if (verificacion.getIntentos() >= MAX_INTENTOS) {
            verificaciones.deleteById(destino);
            return Resultado.AGOTADO;
        }
        if (!BCrypt.checkpw(codigo, verificacion.getCodigoHash())) {
            verificacion.setIntentos(verificacion.getIntentos() + 1);
            if (verificacion.getIntentos() >= MAX_INTENTOS) {
                verificaciones.deleteById(destino);
                return Resultado.AGOTADO;
            }
            verificaciones.save(verificacion);
            return Resultado.INVALIDO;
        }
        Usuario usuario = usuarioOpt.get();
        usuario.setVerificado(true);
        usuarios.save(usuario);
        verificaciones.deleteById(destino);
        return Resultado.VERIFICADA;
    }
}
