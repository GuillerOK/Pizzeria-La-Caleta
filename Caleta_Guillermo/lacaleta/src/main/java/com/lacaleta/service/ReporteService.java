package com.lacaleta.service;

import com.lacaleta.repository.PedidoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class ReporteService {
    private final PedidoRepository pedidos;
    private final Clock reloj;

    @org.springframework.beans.factory.annotation.Autowired
    public ReporteService(PedidoRepository pedidos) {
        this(pedidos, Clock.system(ZoneId.of("America/Lima")));
    }

    ReporteService(PedidoRepository pedidos, Clock reloj) {
        this.pedidos = pedidos;
        this.reloj = reloj;
    }

    public record Periodo(LocalDate desde, LocalDate hasta) {}
    public record Resumen(long pedidos, long entregados, long cancelados, double ventas, double ticketPromedio) {}
    public record Punto(String fecha, double ventas) {}
    public record Pago(String metodo, long pedidos, double importe) {}
    public record Producto(String nombre, long unidades, double importe) {}
    public record Reporte(Periodo periodo, Periodo periodoAnterior, String agrupacion,
                          Resumen resumen, Resumen anterior, Double variacionVentas,
                          List<Punto> tendencia, List<Pago> pagos, List<Producto> productos) {}

    Periodo periodo(String tipo, LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate inicio = switch (tipo) {
            case "hoy" -> hoy;
            case "semana" -> hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "mes" -> hoy.withDayOfMonth(1);
            case "anio" -> hoy.withDayOfYear(1);
            case "personalizado" -> desde;
            default -> throw invalido("Período no válido.");
        };
        LocalDate fin = "personalizado".equals(tipo) ? hasta : hoy;
        if (inicio == null || fin == null) throw invalido("Selecciona ambas fechas.");
        if (inicio.isAfter(fin)) throw invalido("La fecha inicial debe ser anterior o igual a la final.");
        if (fin.isAfter(hoy)) throw invalido("La fecha final no puede estar en el futuro.");
        if (inicio.isBefore(LocalDate.of(2000, 1, 1)) || fin.isAfter(inicio.plusYears(5)))
            throw invalido("Selecciona un intervalo de hasta cinco años desde el año 2000.");
        return new Periodo(inicio, fin);
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Reporte generar(String tipo, LocalDate desde, LocalDate hasta) {
        Periodo periodo = periodo(tipo, desde, hasta);
        long dias = ChronoUnit.DAYS.between(periodo.desde(), periodo.hasta()) + 1;
        Periodo previo = new Periodo(periodo.desde().minusDays(dias), periodo.desde().minusDays(1));
        Resumen resumen = resumir(periodo);
        Resumen anterior = resumir(previo);
        LocalDateTime inicio = periodo.desde().atStartOfDay();
        LocalDateTime fin = periodo.hasta().plusDays(1).atStartOfDay();
        String agrupacion = dias <= 62 ? "dia" : dias <= 180 ? "semana" : "mes";
        Map<LocalDate, Double> puntos = new TreeMap<>();
        for (LocalDate dia = periodo.desde(); !dia.isAfter(periodo.hasta()); dia = dia.plusDays(1))
            puntos.putIfAbsent(agrupar(dia, agrupacion, periodo.desde()), 0.0);
        for (Object[] fila : pedidos.tendenciaReporte(inicio, fin)) {
            LocalDate dia = LocalDate.of(((Number) fila[0]).intValue(), ((Number) fila[1]).intValue(), ((Number) fila[2]).intValue());
            puntos.merge(agrupar(dia, agrupacion, periodo.desde()), ((Number) fila[3]).doubleValue(), Double::sum);
        }
        List<Pago> pagos = pedidos.pagosReporte(inicio, fin).stream()
                .map(f -> new Pago(f[0] == null ? "SIN REGISTRAR" : String.valueOf(f[0]), ((Number) f[1]).longValue(), ((Number) f[2]).doubleValue()))
                .sorted(Comparator.comparingDouble(Pago::importe).reversed().thenComparing(Pago::metodo)).toList();
        List<Producto> productos = pedidos.productosReporte(inicio, fin, PageRequest.of(0, 10)).stream()
                .map(f -> new Producto(String.valueOf(f[1]), ((Number) f[2]).longValue(), ((Number) f[3]).doubleValue())).toList();
        Double variacion = anterior.ventas() == 0 ? null : (resumen.ventas() - anterior.ventas()) * 100 / anterior.ventas();
        return new Reporte(periodo, previo, agrupacion, resumen, anterior, variacion,
                puntos.entrySet().stream().map(e -> new Punto(e.getKey().toString(), e.getValue())).toList(), pagos, productos);
    }

    private Resumen resumir(Periodo periodo) {
        long total = 0, entregados = 0, cancelados = 0;
        double ventas = 0;
        for (Object[] fila : pedidos.resumenReporte(periodo.desde().atStartOfDay(), periodo.hasta().plusDays(1).atStartOfDay())) {
            long cantidad = ((Number) fila[1]).longValue();
            total += cantidad;
            if ("ENTREGADO".equals(fila[0])) {
                entregados += cantidad;
                ventas += ((Number) fila[2]).doubleValue();
            }
            if ("CANCELADO".equals(fila[0])) cancelados += cantidad;
        }
        return new Resumen(total, entregados, cancelados, ventas, entregados == 0 ? 0 : ventas / entregados);
    }

    private LocalDate agrupar(LocalDate dia, String agrupacion, LocalDate inicio) {
        LocalDate grupo = switch (agrupacion) {
            case "semana" -> dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "mes" -> dia.withDayOfMonth(1);
            default -> dia;
        };
        return grupo.isBefore(inicio) ? inicio : grupo;
    }

    private ResponseStatusException invalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
