package com.lacaleta.service;

import com.lacaleta.model.Usuario;
import com.lacaleta.repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Autowired private com.lacaleta.repository.CuponRepository cupones;
    @Autowired private com.lacaleta.repository.PedidoRepository pedidos;
    @Autowired private com.lacaleta.repository.VerificacionCuentaRepository verificaciones;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return usuarioRepository.findByEmail(email.trim().toLowerCase());
    }

    public boolean guardar(Usuario usuario) {
        if (usuario == null || usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            return false;
        }

        // Normalizamos el correo a minusculas y sin espacios laterales
        String emailLimpio = usuario.getEmail().trim().toLowerCase();
        usuario.setEmail(emailLimpio);

        if (usuario.getId() == null) {
            usuario.setVerificado(false);
            // Verificar si el correo ya existe en la BD antes de registrar
            if (usuarioRepository.existsByEmail(emailLimpio)) {
                return false;
            }
            if (usuario.getFechaRegistro() == null) {
                usuario.setFechaRegistro(LocalDateTime.now());
            }
            // Asignar rol CLIENTE (id=1) por defecto en nuevos registros
            usuario.setRolId(1);
            // Todo registro recibe una contraseña en texto y se cifra aquí una sola vez.
            if (usuario.getPassword() != null) {
                usuario.setPassword(BCrypt.hashpw(usuario.getPassword(), BCrypt.gensalt()));
            }
        }

        usuarioRepository.save(usuario);
        return true;
    }

    public boolean verificarUsuario(String email) {
        return buscarPorEmail(email).map(usuario -> {
            usuario.setVerificado(true);
            usuarioRepository.save(usuario);
            return true;
        }).orElse(false);
    }

    /**
     * Autentica las credenciales. El controlador exige además la cuenta verificada.
     */
    public Optional<Usuario> autenticar(String email, String password) {
        if (email == null || password == null) {
            return Optional.empty();
        }

        String emailLimpio = email.trim().toLowerCase();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(emailLimpio);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (usuario.getPassword() != null) {
                try {
                    // Soportar tanto BCrypt como texto plano (migracion)
                    boolean autenticado;
                    if (usuario.getPassword().startsWith("$2a$") || usuario.getPassword().startsWith("$2b$")) {
                        autenticado = BCrypt.checkpw(password.trim(), usuario.getPassword());
                    } else {
                        autenticado = password.trim().equals(usuario.getPassword());
                    }
                    if (autenticado) {
                        return Optional.of(usuario);
                    }
                } catch (Exception e) {
                    // Hash invalido: no autenticar
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Actualiza la contraseña de un usuario, encriptandola con BCrypt.
     */
    public boolean actualizarPassword(String email, String nuevaPassword) {
        if (email == null || nuevaPassword == null || nuevaPassword.isBlank()) {
            return false;
        }

        String emailLimpio = email.trim().toLowerCase();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(emailLimpio);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setPassword(BCrypt.hashpw(nuevaPassword.trim(), BCrypt.gensalt()));
            usuarioRepository.save(usuario);
            return true;
        }
        return false;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Integer id) {
        if (id == null) return Optional.empty();
        return usuarioRepository.findById(id);
    }

    public void eliminar(Usuario usuario) {
        if (usuario != null && usuario.getId() != null) {
            usuarioRepository.delete(usuario);
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void eliminarCuenta(Integer id) {
        Usuario usuario = usuarioRepository.bloquearPorId(id).orElseThrow();
        cupones.deleteByUsuario(usuario);
        verificaciones.deleteById(usuario.getEmail());
        pedidos.anonimizarPorUsuario(id);
        usuarioRepository.delete(usuario);
    }

    /**
     * Cuenta los usuarios con rolId == 1 (CLIENTE).
     */
    public long contarClientes() {
        return usuarioRepository.countByRolId(1);
    }
}
