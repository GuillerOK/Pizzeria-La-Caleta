package com.lacaleta.controller;

import com.lacaleta.model.Cupon;
import com.lacaleta.model.Usuario;
import com.lacaleta.repository.CuponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CuponRestController.class)
class CuponCheckoutTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CuponRepository cupones;

    @MockitoBean
    private com.lacaleta.repository.UsuarioRepository usuarioRepository;

    private Usuario cliente;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        cliente = new Usuario(1, "Guillermo TL", "guille@example.com", "hash", "999999999", "12345678", 1);
        cliente.setVerificado(true);
        cliente.setPuntos(12);
        session = new MockHttpSession();
        session.setAttribute("usuarioLogueado", cliente);
    }

    @Test
    void validarCuponExitosoCalculaDescuento() throws Exception {
        Cupon cupon = new Cupon("CAL5-ABC123", "Cupón S/ 5.00 OFF", "Descuento de 5", "MONTO_FIJO", 5.0, 20, cliente, LocalDateTime.now().plusDays(10));
        when(cupones.findByCodigoIgnoreCase(eq("CAL5-ABC123"))).thenReturn(Optional.of(cupon));

        mvc.perform(post("/api/cupones/validar")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"CAL5-ABC123\",\"subtotal\":25.50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(true))
                .andExpect(jsonPath("$.codigo").value("CAL5-ABC123"))
                .andExpect(jsonPath("$.descuento").value(5.0));
    }

    @Test
    void validarCuponInexistenteRetornaError() throws Exception {
        when(cupones.findByCodigoIgnoreCase(eq("NO_EXISTE"))).thenReturn(Optional.empty());

        mvc.perform(post("/api/cupones/validar")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"NO_EXISTE\",\"subtotal\":25.50}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valido").value(false));
    }

    @Test
    void validarCuponUsadoRetornaError() throws Exception {
        Cupon cupon = new Cupon("CAL5-USADO", "Cupón S/ 5.00 OFF", "Descuento de 5", "MONTO_FIJO", 5.0, 20, cliente, LocalDateTime.now().plusDays(10));
        cupon.setEstado("USADO");
        when(cupones.findByCodigoIgnoreCase(eq("CAL5-USADO"))).thenReturn(Optional.of(cupon));

        mvc.perform(post("/api/cupones/validar")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"CAL5-USADO\",\"subtotal\":25.50}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Este cupón ya fue utilizado o no se encuentra activo."));
    }

    @Test
    void validarCuponDeOtroUsuarioRetornaForbidden() throws Exception {
        Usuario otro = new Usuario(2, "Otro", "otro@example.com", "hash", "987654321", "87654321", 1);
        Cupon cupon = new Cupon("CAL5-OTRO", "Cupón S/ 5.00 OFF", "Descuento de 5", "MONTO_FIJO", 5.0, 20, otro, LocalDateTime.now().plusDays(10));
        when(cupones.findByCodigoIgnoreCase(eq("CAL5-OTRO"))).thenReturn(Optional.of(cupon));

        mvc.perform(post("/api/cupones/validar")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"CAL5-OTRO\",\"subtotal\":25.50}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Este cupón pertenece a otra cuenta de usuario."));
    }

    @Test
    void validarCuponCaseInsensitiveExitoso() throws Exception {
        Cupon cupon = new Cupon("cal5-33543f4d-e9e9-4009-a3e0-a5816", "Cupón S/ 5.00 OFF", "Descuento de 5", "MONTO_FIJO", 5.0, 20, cliente, LocalDateTime.now().plusDays(10));
        when(cupones.findByCodigoIgnoreCase(eq("CAL5-33543F4D-E9E9-4009-A3E0-A5816"))).thenReturn(Optional.of(cupon));

        mvc.perform(post("/api/cupones/validar")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"CAL5-33543F4D-E9E9-4009-A3E0-A5816\",\"subtotal\":30.50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(true))
                .andExpect(jsonPath("$.descuento").value(5.0));
    }
}
