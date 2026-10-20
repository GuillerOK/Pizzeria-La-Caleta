document.addEventListener('DOMContentLoaded', () => {
    const el = id => document.getElementById(id);
    const soles = valor => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(valor);
    const numero = valor => new Intl.NumberFormat('es-PE', { maximumFractionDigits: 2 }).format(valor);
    const fecha = valor => new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'UTC' }).format(new Date(valor + 'T00:00:00Z'));
    const hoyLima = () => {
        const partes = new Intl.DateTimeFormat('en-US', { timeZone: 'America/Lima', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date());
        const valor = tipo => partes.find(p => p.type === tipo).value;
        return `${valor('year')}-${valor('month')}-${valor('day')}`;
    };
    let graficos = [];
    let solicitud = null;

    function actualizarFechas() {
        const hoy = hoyLima();
        el('desde').max = hoy;
        el('hasta').max = hoy;
        const personalizado = el('periodo').value === 'personalizado';
        el('desde').disabled = el('hasta').disabled = !personalizado;
        el('hasta').setCustomValidity('');
        if (personalizado) return;
        const inicio = new Date(hoy + 'T00:00:00Z');
        if (el('periodo').value === 'semana') inicio.setUTCDate(inicio.getUTCDate() - ((inicio.getUTCDay() + 6) % 7));
        if (el('periodo').value === 'mes') inicio.setUTCDate(1);
        if (el('periodo').value === 'anio') inicio.setUTCMonth(0, 1);
        el('desde').value = inicio.toISOString().slice(0, 10);
        el('hasta').value = hoy;
    }

    function tabla(id, filas, columnas, vacio) {
        const cuerpo = el(id);
        cuerpo.replaceChildren();
        if (!filas.length) {
            const celda = document.createElement('td');
            celda.colSpan = columnas;
            celda.textContent = vacio;
            const fila = document.createElement('tr');
            fila.append(celda);
            cuerpo.append(fila);
            return;
        }
        for (const valores of filas) {
            const fila = document.createElement('tr');
            for (const valor of valores) {
                const celda = document.createElement('td');
                celda.textContent = valor;
                fila.append(celda);
            }
            cuerpo.append(fila);
        }
    }

    function mostrar(data) {
        el('tituloPeriodo').textContent = `${fecha(data.periodo.desde)} — ${fecha(data.periodo.hasta)}`;
        el('comparacionPeriodo').textContent = `Comparado con ${fecha(data.periodoAnterior.desde)} — ${fecha(data.periodoAnterior.hasta)} (igual número de días).`;
        el('desde').value = data.periodo.desde;
        el('hasta').value = data.periodo.hasta;
        const resumen = data.resumen;
        el('kpiVentas').textContent = soles(resumen.ventas);
        el('kpiEntregados').textContent = numero(resumen.entregados);
        el('kpiTicket').textContent = soles(resumen.ticketPromedio);
        el('kpiCancelados').textContent = numero(resumen.cancelados);
        el('totalPedidos').textContent = `${numero(resumen.pedidos)} pedidos creados en el período`;
        el('tasaCancelacion').textContent = resumen.pedidos ? `${numero(resumen.cancelados * 100 / resumen.pedidos)} % de los pedidos del período` : 'Sin pedidos en el período';
        const variacion = el('variacionVentas');
        variacion.className = data.variacionVentas > 0 ? 'positivo' : data.variacionVentas < 0 ? 'negativo' : '';
        variacion.textContent = data.variacionVentas === null ? 'Sin base de comparación: ventas anteriores S/ 0.00' : `${data.variacionVentas > 0 ? '+' : ''}${numero(data.variacionVentas)} % frente al período anterior`;
        el('sinDatos').hidden = resumen.pedidos !== 0;
        el('sinVentas').hidden = resumen.pedidos === 0 || resumen.entregados > 0;
        el('agrupacion').textContent = { dia: 'Agrupado por día', semana: 'Agrupado por semana (lunes)', mes: 'Agrupado por mes' }[data.agrupacion];
        tabla('tablaTendencia', data.tendencia.map(p => [fecha(p.fecha), soles(p.ventas)]), 2, 'Sin ventas.');
        tabla('tablaProductos', data.productos.map(p => [p.nombre, numero(p.unidades), soles(p.importe)]), 3, 'Sin productos vendidos en pedidos entregados.');
        tabla('tablaPagos', data.pagos.map(p => [p.metodo.replaceAll('_', ' '), numero(p.pedidos), soles(p.importe)]), 3, 'Sin métodos de pago en pedidos entregados.');
        graficos.forEach(grafico => grafico.destroy());
        graficos = [];
        el('chartTendencia').hidden = el('chartPagos').hidden = typeof Chart === 'undefined';
        if (typeof Chart === 'undefined') return false;
        graficos.push(new Chart(el('chartTendencia'), {
            type: 'line',
            data: { labels: data.tendencia.map(p => fecha(p.fecha)), datasets: [{ label: 'Ventas (S/)', data: data.tendencia.map(p => p.ventas), borderColor: '#e44920', backgroundColor: 'rgba(228,73,32,.1)', fill: true, tension: .2, pointRadius: data.tendencia.length > 40 ? 0 : 3 }] },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false }, tooltip: { callbacks: { label: c => soles(c.parsed.y) } } }, scales: { y: { beginAtZero: true, ticks: { callback: v => soles(v) } }, x: { ticks: { maxTicksLimit: 9 } } } }
        }));
        if (data.pagos.length && data.pagos.some(p => p.importe > 0)) {
            el('chartPagos').hidden = false;
            graficos.push(new Chart(el('chartPagos'), {
                type: 'doughnut', data: { labels: data.pagos.map(p => p.metodo.replaceAll('_', ' ')), datasets: [{ data: data.pagos.map(p => p.importe), backgroundColor: ['#e44920', '#a82541', '#d89224', '#64748b', '#36866b'] }] },
                options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' }, tooltip: { callbacks: { label: c => `${c.label}: ${soles(c.parsed)}` } } } }
            }));
        } else el('chartPagos').hidden = true;
        return true;
    }

    async function cargarReporte(event) {
        if (event) event.preventDefault();
        actualizarFechas();
        if (el('desde').value > el('hasta').value) el('hasta').setCustomValidity('La fecha final debe ser igual o posterior a la inicial.');
        if (!el('filtrosReporte').reportValidity()) return;
        if (solicitud) solicitud.abort();
        const actual = new AbortController();
        solicitud = actual;
        el('resultadoReporte').hidden = true;
        el('resultadoReporte').setAttribute('aria-busy', 'true');
        el('estadoReporte').className = 'reporte-estado';
        el('estadoReporte').textContent = 'Cargando reporte…';
        el('aplicarReporte').disabled = true;
        const parametros = new URLSearchParams({ periodo: el('periodo').value });
        if (el('periodo').value === 'personalizado') {
            parametros.set('desde', el('desde').value);
            parametros.set('hasta', el('hasta').value);
        }
        try {
            const respuesta = await fetch('/api/admin/reportes?' + parametros, { signal: actual.signal, headers: { Accept: 'application/json' } });
            if (respuesta.redirected || respuesta.status === 401 || respuesta.status === 403) throw new Error('Tu sesión no permite consultar reportes. Vuelve a ingresar desde el panel.');
            if (!respuesta.ok) throw new Error(respuesta.status === 400 ? 'Revisa las fechas: no se permiten fechas futuras ni intervalos mayores a cinco años.' : 'No se pudo cargar el reporte. Vuelve a aplicar los filtros para reintentar.');
            const data = await respuesta.json();
            if (solicitud !== actual) return;
            el('resultadoReporte').hidden = false;
            const conGraficos = mostrar(data);
            el('estadoReporte').textContent = conGraficos ? 'Reporte actualizado.' : 'Reporte actualizado. Los gráficos no están disponibles; puedes consultar las cifras en las tablas.';
        } catch (error) {
            if (error.name === 'AbortError') return;
            el('resultadoReporte').hidden = true;
            el('estadoReporte').className = 'reporte-estado error';
            el('estadoReporte').textContent = error.message || 'No se pudo cargar el reporte.';
        } finally {
            if (solicitud === actual) {
                el('aplicarReporte').disabled = false;
                el('resultadoReporte').setAttribute('aria-busy', 'false');
            }
        }
    }

    async function cargarInventario() {
        el('actualizarInventario').disabled = true;
        el('estadoInventario').textContent = 'Cargando inventario…';
        el('tablaInventario').replaceChildren();
        try {
            const respuesta = await fetch('/api/admin/reportes/inventario', { headers: { Accept: 'application/json' } });
            if (!respuesta.ok || respuesta.redirected) throw new Error('No se pudo consultar el inventario. Reintenta con Actualizar inventario.');
            const datos = await respuesta.json();
            tabla('tablaInventario', datos.map(i => [i.nombre, `${numero(i.cantidad)} ${i.unidad || ''}`, i.minimo === null ? 'Sin definir' : `${numero(i.minimo)} ${i.unidad || ''}`, i.critico ? 'Crítico' : i.minimo === null ? 'Sin umbral' : 'Sobre el mínimo']), 4, 'No hay insumos registrados.');
            el('estadoInventario').textContent = 'Consultado: ' + new Intl.DateTimeFormat('es-PE', { timeZone: 'America/Lima', dateStyle: 'short', timeStyle: 'short' }).format(new Date());
        } catch (error) {
            el('estadoInventario').textContent = error.message;
        } finally { el('actualizarInventario').disabled = false; }
    }

    function filtrosPendientes() {
        if (solicitud) solicitud.abort();
        solicitud = null;
        el('aplicarReporte').disabled = false;
        el('hasta').setCustomValidity('');
        el('resultadoReporte').hidden = true;
        el('resultadoReporte').setAttribute('aria-busy', 'false');
        el('estadoReporte').className = 'reporte-estado';
        el('estadoReporte').textContent = 'Aplica los filtros para consultar este período.';
    }
    el('periodo').addEventListener('change', () => { actualizarFechas(); filtrosPendientes(); });
    el('hasta').addEventListener('input', filtrosPendientes);
    el('desde').addEventListener('input', filtrosPendientes);
    el('filtrosReporte').addEventListener('submit', cargarReporte);
    el('actualizarInventario').addEventListener('click', cargarInventario);
    actualizarFechas();
    cargarReporte();
    cargarInventario();
});
