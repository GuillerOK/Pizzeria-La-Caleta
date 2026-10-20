const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const recursos = path.resolve(__dirname, '../../main/resources');
const reporte = () => ({ periodo: { desde: '2026-01-01', hasta: '2026-01-02' }, periodoAnterior: { desde: '2025-12-30', hasta: '2025-12-31' }, agrupacion: 'dia', resumen: { pedidos: 2, entregados: 1, cancelados: 1, ventas: 45, ticketPromedio: 45 }, variacionVentas: null, tendencia: [{ fecha: '2026-01-01', ventas: 45 }], productos: [{ nombre: '<img src=x onerror=evil>', unidades: 1, importe: 40 }], pagos: [{ metodo: 'YAPE', pedidos: 1, importe: 45 }] });

function entorno() {
    const elementos = new Map();
    function nodo() {
        return { value: '', children: [], listeners: {}, hidden: false, disabled: false, textContent: '', validity: '',
            append(child) { this.children.push(child); }, replaceChildren() { this.children = []; },
            setAttribute() {}, setCustomValidity(texto) { this.validity = texto; }, reportValidity() { return true; },
            addEventListener(tipo, listener) { this.listeners[tipo] = listener; } };
    }
    const html = fs.readFileSync(path.join(recursos, 'templates/admin/reportes.html'), 'utf8');
    for (const match of html.matchAll(/id="([^"]+)"/g)) elementos.set(match[1], nodo());
    elementos.get('periodo').value = 'mes';
    const solicitudes = [];
    let iniciar;
    let siguiente = async () => ({ ok: true, json: async () => reporte() });
    const contexto = vm.createContext({ Intl, Date, URLSearchParams, AbortController,
        document: { getElementById: id => { assert.ok(elementos.has(id), `Falta ${id}`); return elementos.get(id); }, createElement: nodo, addEventListener: (_, f) => { iniciar = f; } },
        fetch: async (url, opciones) => {
            solicitudes.push({ url, opciones });
            if (url.endsWith('/inventario')) return { ok: true, json: async () => [] };
            return siguiente();
        }
    });
    vm.runInContext(fs.readFileSync(path.join(recursos, 'static/js/reporte.js'), 'utf8'), contexto);
    iniciar();
    return { el: id => elementos.get(id), solicitudes, responder: fn => { siguiente = fn; }, submit: () => elementos.get('filtrosReporte').listeners.submit({ preventDefault() {} }) };
}
const completar = () => new Promise(resolve => setImmediate(resolve));

test('muestra cifras sin Chart.js, conserva nombres como texto y no inventa variación con base cero', async () => {
    const e = entorno(); await completar();
    assert.equal(e.el('resultadoReporte').hidden, false);
    assert.match(e.el('estadoReporte').textContent, /tablas/);
    assert.match(e.el('variacionVentas').textContent, /Sin base/);
    assert.equal(e.el('tablaProductos').children[0].children[0].textContent, '<img src=x onerror=evil>');
    assert.match(e.solicitudes[0].url, /periodo=mes/);
});

test('rango personalizado conserva fechas elegidas y oculta resultados anteriores al editar', async () => {
    const e = entorno(); await completar();
    e.el('periodo').value = 'personalizado'; e.el('periodo').listeners.change();
    assert.equal(e.el('resultadoReporte').hidden, true);
    assert.equal(e.el('desde').disabled, false);
    e.el('desde').value = '2025-04-01'; e.el('hasta').value = '2025-04-30';
    await e.submit();
    assert.match(e.solicitudes.at(-1).url, /periodo=personalizado&desde=2025-04-01&hasta=2025-04-30/);
});

test('error HTTP oculta cifras viejas y permite reintentar', async () => {
    const e = entorno(); await completar();
    e.responder(async () => ({ ok: false, status: 500 }));
    await e.submit();
    assert.equal(e.el('resultadoReporte').hidden, true);
    assert.equal(e.el('aplicarReporte').disabled, false);
    assert.match(e.el('estadoReporte').textContent, /reintentar/);
});

test('una respuesta pendiente no puede sobrescribir filtros que el usuario acaba de editar', async () => {
    const e = entorno(); await completar();
    let resolver;
    e.responder(() => new Promise(resolve => { resolver = resolve; }));
    const pendiente = e.submit();
    e.el('periodo').value = 'personalizado'; e.el('periodo').listeners.change();
    e.el('desde').value = '2025-05-01';
    resolver({ ok: true, json: async () => reporte() });
    await pendiente;
    assert.equal(e.el('desde').value, '2025-05-01');
    assert.equal(e.el('resultadoReporte').hidden, true);
    assert.equal(e.el('aplicarReporte').disabled, false);
});
