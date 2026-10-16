package com.lacaleta.service;

import com.lacaleta.model.Usuario;
import com.lacaleta.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioVerificationTests {
    @Test
    void nuevoUsuarioSiempreEsClientePendienteYPasswordSeEncripta() {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        UsuarioService service = new UsuarioService(repository);
        Usuario usuario = new Usuario(null, "Camila Torres", " CLIENTE@example.com ", "ClaveSegura1!", "987654321", "72839401", 2);
        usuario.setVerificado(true);
        assertTrue(service.guardar(usuario));
        assertEquals(false, usuario.getVerificado());
        assertEquals(1, usuario.getRolId());
        assertEquals("cliente@example.com", usuario.getEmail());
        assertTrue(BCrypt.checkpw("ClaveSegura1!", usuario.getPassword()));
        verify(repository).save(usuario);
    }

    @Test
    void verificarPersisteEstadoYEditarPerfilLoConserva() {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        UsuarioService service = new UsuarioService(repository);
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setEmail("cliente@example.com");
        when(repository.findByEmail("cliente@example.com")).thenReturn(Optional.of(usuario));
        assertTrue(service.verificarUsuario(" CLIENTE@example.com "));
        assertTrue(usuario.getVerificado());
        assertTrue(service.guardar(usuario));
        assertTrue(usuario.getVerificado());
        verify(repository, times(2)).save(usuario);
    }

    @Test
    void verificarUsuarioInexistenteNoGuarda() {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());
        assertFalse(new UsuarioService(repository).verificarUsuario("nadie@example.com"));
        verify(repository, never()).save(any());
    }
}
