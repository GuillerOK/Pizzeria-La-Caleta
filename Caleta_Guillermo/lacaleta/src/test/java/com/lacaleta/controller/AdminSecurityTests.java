package com.lacaleta.controller;

import com.lacaleta.model.Usuario;
import com.lacaleta.repository.*;
import com.lacaleta.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AdminRestController.class, AdminProductoController.class, AdminInsumoController.class, ReporteController.class})
class AdminSecurityTests {
    @Autowired MockMvc mvc;
    @MockitoBean PedidoRepository pedidos;
    @MockitoBean ProductoRepository productos;
    @MockitoBean InsumoRepository insumos;
    @MockitoBean UsuarioRepository usuarios;
    @MockitoBean ProductoService productoService;
    @MockitoBean InsumoService insumoService;
    @MockitoBean CategoriaService categorias;
    @MockitoBean ReporteService reportes;
    MockHttpSession sesion(int rol) {
        var session = new MockHttpSession(); var usuario = new Usuario();
        usuario.setId(2);
        usuario.setRolId(rol); usuario.setVerificado(true);
        when(usuarios.findById(2)).thenReturn(java.util.Optional.of(usuario));
        session.setAttribute("usuarioLogueado", usuario); return session;
    }
    @Test void apiSinSesionDevuelve401() throws Exception {
        mvc.perform(get("/api/admin/reportes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/reportes/inventario")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/dashboard/metricas")).andExpect(status().isUnauthorized());
        verifyNoInteractions(pedidos, productos, insumos, usuarios);
    }
    @Test void clienteNoAccedeAMetricasNiMutaciones() throws Exception {
        var sesion = sesion(1);
        mvc.perform(get("/api/admin/reportes").session(sesion)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/reportes/inventario").session(sesion)).andExpect(status().isForbidden());
        verifyNoInteractions(reportes);
        mvc.perform(get("/api/admin/dashboard/metricas").session(sesion)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/pizzas/guardar").session(sesion)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/insumos/guardar").session(sesion)).andExpect(status().isForbidden());
        verifyNoInteractions(productoService, insumoService, pedidos);
    }
    @Test void adminVerificadoPuedeConsultarDatos() throws Exception {
        mvc.perform(get("/api/admin/reportes").session(sesion(2))).andExpect(status().isOk());
        verify(reportes).generar("mes", null, null);
        mvc.perform(get("/api/admin/reportes/inventario").session(sesion(2))).andExpect(status().isOk());
        mvc.perform(get("/api/admin/dashboard/metricas").session(sesion(2))).andExpect(status().isOk());
        verify(pedidos, never()).findAll();
        verify(usuarios).countByRolId(1);
    }
    @Test void origenExternoNoPuedeMutarConCookieAdmin() throws Exception {
        mvc.perform(post("/admin/pizzas/guardar").session(sesion(2)).header("Origin", "https://externo.example"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(productoService);
    }
    @Test void getNoEliminaPizzas() throws Exception {
        mvc.perform(get("/admin/pizzas/eliminar/1").session(sesion(2))).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(productoService);
    }

    @Test void rechazaFormatoDeFechaInvalido() throws Exception {
        mvc.perform(get("/api/admin/reportes").session(sesion(2)).param("periodo", "personalizado").param("desde", "invalido"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reportes);
    }
    @Test void permiteGestionarMasaMedidaEnUnidades() throws Exception {
        mvc.perform(post("/admin/insumos/guardar").session(sesion(2))
                .param("nombre", "Masa Artesanal").param("stockActual", "25")
                .param("stockMinimo", "10").param("unidadMedida", "unidades"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("exito"));
        verify(insumoService).guardar(argThat(i -> "unidades".equals(i.getUnidadMedida())));
    }
}
