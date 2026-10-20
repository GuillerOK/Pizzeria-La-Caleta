package com.lacaleta.service;

import com.lacaleta.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReporteServiceTests {
    PedidoRepository pedidos = mock(PedidoRepository.class);
    // En UTC ya es lunes; en Lima todavía es domingo.
    ReporteService servicio = new ReporteService(pedidos,
            Clock.fixed(Instant.parse("2026-10-19T02:00:00Z"), ZoneId.of("America/Lima")));

    @Test void periodosRapidosUsanLimaYLunesComoInicioDeSemana() {
        assertEquals(new ReporteService.Periodo(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18)), servicio.periodo("semana", null, null));
        assertEquals(LocalDate.of(2026, 10, 18), servicio.periodo("hoy", null, null).desde());
        assertEquals(LocalDate.of(2026, 10, 1), servicio.periodo("mes", null, null).desde());
        assertEquals(LocalDate.of(2026, 1, 1), servicio.periodo("anio", null, null).desde());
    }

    @Test void rechazaFechasInvalidasAntesDeConsultar() {
        LocalDate hoy = LocalDate.of(2026, 10, 18);
        assertThrows(ResponseStatusException.class, () -> servicio.generar("otro", null, null));
        assertThrows(ResponseStatusException.class, () -> servicio.generar("personalizado", null, hoy));
        assertThrows(ResponseStatusException.class, () -> servicio.generar("personalizado", hoy, hoy.minusDays(1)));
        assertThrows(ResponseStatusException.class, () -> servicio.generar("personalizado", hoy, hoy.plusDays(1)));
        assertThrows(ResponseStatusException.class, () -> servicio.generar("personalizado", hoy.minusYears(6), hoy));
        verifyNoInteractions(pedidos);
    }

    @Test void sinVentasNoDividePorCeroYCompletaDiasSinDatos() {
        var reporte = servicio.generar("semana", null, null);
        assertEquals(0, reporte.resumen().ticketPromedio());
        assertNull(reporte.variacionVentas());
        assertEquals(7, reporte.tendencia().size());
        assertEquals(LocalDate.of(2026, 10, 5), reporte.periodoAnterior().desde());
        assertEquals(LocalDate.of(2026, 10, 11), reporte.periodoAnterior().hasta());
        assertTrue(reporte.tendencia().stream().allMatch(p -> p.ventas() == 0));
    }

    @Test void intervalosLargosAgrupanPorMesSinCrearPuntosFueraDelRango() {
        var reporte = servicio.generar("personalizado", LocalDate.of(2025, 1, 15), LocalDate.of(2025, 12, 31));
        assertEquals("mes", reporte.agrupacion());
        assertEquals(12, reporte.tendencia().size());
        assertEquals("2025-01-15", reporte.tendencia().get(0).fecha());
    }
}
