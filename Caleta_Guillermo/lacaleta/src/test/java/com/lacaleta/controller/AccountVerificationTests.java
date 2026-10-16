package com.lacaleta.controller;

import com.lacaleta.model.Usuario;
import com.lacaleta.model.VerificacionCuenta;
import com.lacaleta.repository.UsuarioRepository;
import com.lacaleta.repository.VerificacionCuentaRepository;
import com.lacaleta.service.EmailService;
import com.lacaleta.service.UsuarioService;
import com.lacaleta.service.VerificacionCuentaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountVerificationTests {
    private PasswordResetController controller;
    private UsuarioService usuarios;
    private EmailService emails;
    private UsuarioRepository repository;
    private VerificacionCuentaRepository verificaciones;
    private Map<String, VerificacionCuenta> persistidos;
    private static final String EMAIL = "cliente@example.com";

    @BeforeEach
    void setUp() {
        controller = new PasswordResetController();
        usuarios = mock(UsuarioService.class);
        emails = mock(EmailService.class);
        repository = mock(UsuarioRepository.class);
        persistidos = new HashMap<>();
        verificaciones = mock(VerificacionCuentaRepository.class);
        when(repository.buscarPorEmailParaVerificacion(EMAIL)).thenReturn(Optional.of(new Usuario()));
        when(verificaciones.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(persistidos.get(inv.getArgument(0))));
        when(verificaciones.saveAndFlush(any())).thenAnswer(inv -> {
            VerificacionCuenta registro = inv.getArgument(0);
            persistidos.put(registro.getEmail(), registro);
            return registro;
        });
        doAnswer(inv -> { persistidos.remove(inv.getArgument(0)); return null; })
                .when(verificaciones).deleteById(anyString());
        ReflectionTestUtils.setField(controller, "usuarioService", usuarios);
        ReflectionTestUtils.setField(controller, "emailService", emails);
        ReflectionTestUtils.setField(controller, "verificacionCuentaService",
                new VerificacionCuentaService(repository, verificaciones, emails));
        when(usuarios.guardar(any())).thenReturn(true);
        when(emails.enviarCodigoVerificacion(anyString(), anyString())).thenReturn(true);
    }

    private Map<String, String> registro() {
        return new HashMap<>(Map.of(
                "nombre", "Camila", "apellidos", "Torres", "tipoDocumento", "DNI",
                "numeroDocumento", "72839401", "telefono", "987654321",
                "fechaNacimiento", "2000-01-01", "email", " CLIENTE@example.com ",
                "password", "ClaveSegura1!", "confirmPassword", "ClaveSegura1!", "aceptaTerminos", "true"));
    }

    private String registrarYCapturarOtp() {
        assertEquals(HttpStatus.CREATED, controller.register(registro()).getStatusCode());
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emails).enviarCodigoVerificacion(eq(EMAIL), codigo.capture());
        assertTrue(codigo.getValue().matches("[0-9]{6}"));
        return codigo.getValue();
    }

    @Test
    void registraVerificaYConsumeCodigoUnaSolaVez() {
        String otp = registrarYCapturarOtp();
        Map<String, String> payload = Map.of("email", " CLIENTE@example.com ", "otp", otp);
        assertEquals(HttpStatus.OK, controller.verifyAccount(payload).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controller.verifyAccount(payload).getStatusCode());
        verify(repository, times(1)).save(argThat(usuario -> Boolean.TRUE.equals(usuario.getVerificado())));
        assertFalse(persistidos.containsKey(EMAIL));
    }

    @Test
    void guardaSoloHashYUnaInstanciaNuevaPuedeVerificarDesdeRepositorio() {
        Instant antes = Instant.now();
        String codigo = registrarYCapturarOtp();
        VerificacionCuenta guardada = persistidos.get(EMAIL);
        assertNotEquals(codigo, guardada.getCodigoHash());
        assertTrue(BCrypt.checkpw(codigo, guardada.getCodigoHash()));
        assertTrue(guardada.getVencimiento().isAfter(antes.plusSeconds(590)));
        VerificacionCuentaService nuevaInstancia = new VerificacionCuentaService(repository, verificaciones, emails);
        assertEquals(VerificacionCuentaService.Resultado.VERIFICADA, nuevaInstancia.verificar(EMAIL, codigo));
        assertFalse(persistidos.containsKey(EMAIL));
    }

    @Test
    void codigoIncorrectoNoActivaCuentaYLlegaAlLimite() {
        String otp = registrarYCapturarOtp();
        String incorrecto = otp.equals("000000") ? "000001" : "000000";
        for (int i = 0; i < 4; i++) {
            assertEquals(HttpStatus.BAD_REQUEST,
                    controller.verifyAccount(Map.of("email", EMAIL, "otp", incorrecto)).getStatusCode());
        }
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,
                controller.verifyAccount(Map.of("email", EMAIL, "otp", incorrecto)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.verifyAccount(Map.of("email", EMAIL, "otp", otp)).getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaCodigoVencidoYLoElimina() {
        persistidos.put(EMAIL, new VerificacionCuenta(EMAIL,
                BCrypt.hashpw("123456", BCrypt.gensalt()), Instant.now().minusSeconds(1)));
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.verifyAccount(Map.of("email", EMAIL, "otp", "123456")).getStatusCode());
        assertFalse(persistidos.containsKey(EMAIL));
        verify(repository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void codigoDeRecuperacionNoActivaCuenta() {
        ConcurrentHashMap<String, PasswordResetController.RecoveryOtp> recuperacion = (ConcurrentHashMap<String, PasswordResetController.RecoveryOtp>)
                ReflectionTestUtils.getField(controller, "otpCache");
        recuperacion.put(EMAIL, new PasswordResetController.RecoveryOtp("123456", Instant.now().plusSeconds(600)));
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.verifyAccount(Map.of("email", EMAIL, "otp", "123456")).getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void falloSmtpDevuelve503YNoDejaCodigoActivo() {
        when(emails.enviarCodigoVerificacion(anyString(), anyString())).thenReturn(false);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, controller.register(registro()).getStatusCode());
        assertTrue(persistidos.isEmpty());
    }

    @Test
    void permiteReenvioSoloConCredencialesDeCuentaPendiente() {
        when(usuarios.guardar(any())).thenReturn(false);
        when(usuarios.autenticar(EMAIL, "ClaveSegura1!")).thenReturn(Optional.of(new Usuario()));
        assertEquals(HttpStatus.OK, controller.register(registro()).getStatusCode());
        verify(emails).enviarCodigoVerificacion(eq(EMAIL), anyString());
    }

    @Test
    void correoDuplicadoVerificadoDevuelve409() {
        Usuario existente = new Usuario();
        existente.setVerificado(true);
        when(usuarios.guardar(any())).thenReturn(false);
        when(usuarios.autenticar(EMAIL, "ClaveSegura1!")).thenReturn(Optional.of(existente));
        assertEquals(HttpStatus.CONFLICT, controller.register(registro()).getStatusCode());
        verifyNoInteractions(emails);
    }

    @Test
    void loginPendienteNoCreaSesionInclusoSiEstadoEsNull() {
        Usuario usuario = new Usuario();
        MockHttpSession session = new MockHttpSession();
        when(usuarios.autenticar(EMAIL, "clave")).thenReturn(Optional.of(usuario));
        for (Boolean estado : new Boolean[]{false, null}) {
            usuario.setVerificado(estado);
            assertEquals(HttpStatus.FORBIDDEN,
                    controller.login(Map.of("email", EMAIL, "password", "clave"), session).getStatusCode());
            assertNull(session.getAttribute("usuarioLogueado"));
        }
    }

    @Test
    void loginVerificadoCreaSesion() {
        Usuario usuario = new Usuario();
        usuario.setVerificado(true);
        MockHttpSession session = new MockHttpSession();
        when(usuarios.autenticar(EMAIL, "clave")).thenReturn(Optional.of(usuario));
        assertEquals(HttpStatus.OK,
                controller.login(Map.of("email", EMAIL, "password", "clave"), session).getStatusCode());
        assertSame(usuario, session.getAttribute("usuarioLogueado"));
    }

    @Test
    void datosIncompletosDevuelven400() {
        assertEquals(HttpStatus.BAD_REQUEST, controller.register(Map.of()).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controller.verifyAccount(Map.of()).getStatusCode());
        verifyNoInteractions(emails);
    }
}
