package com.lacaleta.controller;

import com.lacaleta.model.Usuario;
import com.lacaleta.repository.UsuarioRepository;
import com.lacaleta.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PasswordRecoveryTests {
    private PasswordResetController controller;
    private UsuarioRepository usuarios;
    private EmailService emails;
    private Usuario usuario;
    private static final String EMAIL = "cliente@example.com";

    @BeforeEach
    void preparar() {
        controller = new PasswordResetController();
        usuarios = mock(UsuarioRepository.class);
        emails = mock(EmailService.class);
        usuario = new Usuario();
        usuario.setPassword(BCrypt.hashpw("Anterior1!", BCrypt.gensalt()));
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(emails.enviarCodigoRecuperacion(eq(EMAIL), anyString())).thenReturn(true);
        ReflectionTestUtils.setField(controller, "usuarioRepository", usuarios);
        ReflectionTestUtils.setField(controller, "emailService", emails);
    }

    private String solicitar() {
        assertEquals(HttpStatus.OK, controller.forgotPassword(Map.of("email", " CLIENTE@example.com ")).getStatusCode());
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emails).enviarCodigoRecuperacion(eq(EMAIL), codigo.capture());
        assertTrue(codigo.getValue().matches("[0-9]{6}"));
        return codigo.getValue();
    }

    @Test
    void codigoEnviadoPorCorreoAutorizaCambioRealYSeConsume() {
        String codigo = solicitar();
        assertEquals(HttpStatus.OK, controller.verifyOtp(Map.of("email", EMAIL, "otp", codigo)).getStatusCode());
        assertEquals(HttpStatus.OK, controller.resetPassword(Map.of(
                "email", EMAIL, "otp", codigo, "newPassword", "NuevaClave1!")).getStatusCode());
        assertTrue(BCrypt.checkpw("NuevaClave1!", usuario.getPassword()));
        verify(usuarios).save(usuario);
        assertEquals(HttpStatus.BAD_REQUEST, controller.verifyOtp(Map.of("email", EMAIL, "otp", codigo)).getStatusCode());
    }

    @Test
    void errorSmtpNoAnunciaEnvioYElCodigoQuedaInvalidado() {
        when(emails.enviarCodigoRecuperacion(eq(EMAIL), anyString())).thenReturn(false);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                controller.forgotPassword(Map.of("email", EMAIL)).getStatusCode());
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emails).enviarCodigoRecuperacion(eq(EMAIL), codigo.capture());
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.verifyOtp(Map.of("email", EMAIL, "otp", codigo.getValue())).getStatusCode());
        verify(usuarios, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void codigoVencidoNoPermiteVerificarNiCambiarPassword() {
        ConcurrentHashMap<String, PasswordResetController.RecoveryOtp> cache =
                (ConcurrentHashMap<String, PasswordResetController.RecoveryOtp>) ReflectionTestUtils.getField(controller, "otpCache");
        cache.put(EMAIL, new PasswordResetController.RecoveryOtp("012345", Instant.now().minusSeconds(1)));
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.verifyOtp(Map.of("email", EMAIL, "otp", "012345")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controller.resetPassword(Map.of(
                "email", EMAIL, "otp", "012345", "newPassword", "NuevaClave1!")).getStatusCode());
        verify(usuarios, never()).save(any());
    }

    @Test
    void rechazaPasswordActualComparandolaConHashDeLaBaseDeDatos() {
        String codigo = solicitar();
        assertEquals(HttpStatus.BAD_REQUEST, controller.resetPassword(Map.of(
                "email", EMAIL, "otp", codigo, "newPassword", "Anterior1!")).getStatusCode());
        verify(usuarios, never()).save(any());
        assertEquals(HttpStatus.OK, controller.verifyOtp(Map.of("email", EMAIL, "otp", codigo)).getStatusCode());
    }

    @Test
    void codigoNuncaSolicitadoNoAutorizaCambio() {
        assertEquals(HttpStatus.BAD_REQUEST, controller.resetPassword(Map.of(
                "email", EMAIL, "otp", "123456", "newPassword", "NuevaClave1!")).getStatusCode());
        verify(usuarios, never()).save(any());
    }
}
