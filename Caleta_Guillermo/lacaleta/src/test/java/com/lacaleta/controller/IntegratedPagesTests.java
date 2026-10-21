package com.lacaleta.controller;

import com.lacaleta.model.*;
import com.lacaleta.repository.*;
import com.lacaleta.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Renderiza las vistas Thymeleaf reales, sin conexión a Neon ni SMTP.
@WebMvcTest(PageController.class)
class IntegratedPagesTests {
    @Autowired MockMvc mvc;
    @MockitoBean ProductoService productos;
    @MockitoBean PedidoRepository pedidos;
    @MockitoBean CategoriaService categorias;
    @MockitoBean InsumoService insumos;
    @MockitoBean UsuarioService usuarios;
    @MockitoBean CuponService cupones;
    @MockitoBean UsuarioRepository usuarioRepository;
    private Usuario cliente;
    private MockHttpSession session;

    @BeforeEach
    void preparar() {
        cliente = new Usuario(10, "Camila Torres", "camila@example.com", "hash", "987654321", "72839401", 1);
        cliente.setVerificado(true);
        session = new MockHttpSession();
        session.setAttribute("usuarioLogueado", cliente);
        when(usuarios.buscarPorId(10)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(cliente));
        when(pedidos.findByUsuarioId(eq(10), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(cupones.calcularPuntosDisponibles(eq(cliente), anyDouble())).thenReturn(50);
        when(cupones.obtenerCatalogo()).thenReturn(new CuponService(mock(CuponRepository.class)).obtenerCatalogo());
    }

    @Test
    void perfilRenderizaCuponesEHistorialSoloDelCliente() throws Exception {
        Pedido propio = new Pedido();
        propio.setId(101);
        propio.setUsuario(cliente);
        propio.setTotal(45.0);
        Pedido ajeno = new Pedido();
        ajeno.setId(202);
        ajeno.setUsuario(new Usuario(20, "Otro", "otro@example.com", "hash", null, null, 1));
        ajeno.setTotal(100.0);
        when(pedidos.findByUsuarioId(eq(10), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(propio)));
        when(pedidos.totalPorUsuario(10)).thenReturn(45.0);
        when(pedidos.findFirstByUsuarioIdOrderByFechaDesc(10)).thenReturn(Optional.of(propio));
        Cupon cupon = new Cupon("CAL5-1234", "Descuento", "Ahorra cinco soles", "MONTO_FIJO", 5.0, 20, cliente, LocalDateTime.now().plusDays(30));
        when(cupones.listarPorUsuario(cliente)).thenReturn(List.of(cupon));
        when(cupones.listarActivos(cliente)).thenReturn(List.of(cupon));
        mvc.perform(get("/perfil").param("tab", "pedidos").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pedidos", List.of(propio)))
                .andExpect(model().attribute("totalGastado", 45.0))
                .andExpect(content().string(containsString("#ORD-101")))
                .andExpect(content().string(not(containsString("#ORD-202"))))
                .andExpect(content().string(containsString("CAL5-1234")));
        verify(pedidos).findByUsuarioId(eq(10), any());
        verify(pedidos, never()).findAll();
    }

    @Test
    void perfilSinPedidosRenderizaYConservaProteccionDeSesion() throws Exception {
        mvc.perform(get("/perfil")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/perfil").session(session)).andExpect(status().isOk())
                .andExpect(model().attribute("totalPedidos", 0L));
    }
    @Test
    void carritoRenderizaModalCompartidoConCuponActivoYLargo() throws Exception {
        var cupon = new Cupon("CAL5-12345678-1234-1234-1234-123456789abc", "Mi descuento", "Cinco soles", "MONTO_FIJO", 5.0, 20, cliente, LocalDateTime.now().plusDays(1));
        when(cupones.listarPorUsuario(cliente)).thenReturn(List.of(cupon));
        mvc.perform(get("/carrito").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"modalMisCupones\"")))
                .andExpect(content().string(containsString("Ver mis cupones")))
                .andExpect(content().string(containsString(cupon.getCodigo())))
                .andExpect(content().string(containsString("data-codigo=")));
    }
    @Test
    void carritoSinCuponesMuestraMensajeVacio() throws Exception {
        mvc.perform(get("/carrito").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Aún no tienes cupones canjeados")));
    }

    @Test
    void adminRenderizaFormulariosDeInsumosYClientes() throws Exception {
        cliente.setRolId(2);
        Insumo insumo = new Insumo();
        insumo.setId(1); insumo.setCodigo("INS-01"); insumo.setNombre("Harina");
        insumo.setStockActual(10.0); insumo.setStockMinimo(5.0); insumo.setUnidadMedida("kg");
        when(insumos.listarConFiltro(null)).thenReturn(List.of(insumo));
        when(usuarios.listar()).thenReturn(List.of(new Usuario(20, "Cliente", "otro@example.com", "hash", "987654321", "72839401", 1)));
        mvc.perform(get("/admin").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin/insumos/eliminar/1")))
                .andExpect(content().string(containsString("/admin/clientes/eliminar/20")));
    }

    @Test
    void registroYCarritoConservanModalesYCorreoDeSesion() throws Exception {
        mvc.perform(get("/registro")).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"modalVerificacionCuenta\"")))
                .andExpect(content().string(containsString("id=\"modalNotificacionGlobal\"")));
        mvc.perform(get("/carrito").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("data-email=\"camila@example.com\"")))
                .andExpect(content().string(containsString("id=\"btnComprar\"")));
    }

    @Test
    void actualizarPerfilConservaVerificacionYPuntos() throws Exception {
        when(usuarios.guardar(cliente)).thenReturn(true);
        mvc.perform(post("/perfil/actualizar").session(session).param("nombre", "Camila Torres")
                .param("telefono", "912345678").param("dni", "72839401"))
                .andExpect(redirectedUrl("/perfil?tab=datos"));
        org.junit.jupiter.api.Assertions.assertTrue(cliente.getVerificado());
        org.junit.jupiter.api.Assertions.assertEquals(50, cliente.getPuntos());
        verify(usuarios).guardar(cliente);
    }

    @Test void checkoutRenderizaFormularioRealYDatosDeSesion() throws Exception {
        mvc.perform(get("/envio")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/pago")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/envio").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"usuarioCheckout\"")));
        mvc.perform(get("/pago").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnFinalizarPedido\"")))
                .andExpect(content().string(containsString("/js/auth.js")))
                .andExpect(content().string(containsString("data-email=\"camila@example.com\"")));
    }

    @Test void paginasPublicasConservanFragmentosValidos() throws Exception {
        for (String ruta : List.of("/", "/menu", "/promociones", "/locales", "/faq", "/terminos", "/delivery", "/login")) {
            mvc.perform(get(ruta)).andExpect(status().isOk());
        }
    }
}
