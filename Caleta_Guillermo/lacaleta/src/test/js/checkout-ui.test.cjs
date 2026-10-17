const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const recursos = path.resolve(__dirname, '../../main/resources');
function archivos(dir) {
    return fs.readdirSync(dir, { withFileTypes: true }).flatMap(e =>
        e.isDirectory() ? archivos(path.join(dir, e.name)) : [path.join(dir, e.name)]);
}
test('ningún JavaScript externo o incrustado utiliza diálogos nativos', () => {
    for (const archivo of archivos(recursos).filter(p => /\.(js|html)$/.test(p))) {
        assert.doesNotMatch(fs.readFileSync(archivo, 'utf8'), /\b(?:alert|confirm|prompt)\s*\(/, archivo);
    }
});
test('cada reemplazo de fragmento Thymeleaf tiene una definición existente', () => {
    const templates = path.join(recursos, 'templates');
    let cantidad = 0;
    for (const archivo of archivos(templates).filter(p => p.endsWith('.html'))) {
        const html = fs.readFileSync(archivo, 'utf8');
        for (const referencia of html.matchAll(/th:replace="~\{([\w/-]+)\s*::\s*(\w+)/g)) {
            const destino = path.join(templates, referencia[1] + '.html');
            assert.ok(fs.existsSync(destino), `${archivo}: ${destino}`);
            assert.match(fs.readFileSync(destino, 'utf8'), new RegExp('th:fragment="' + referencia[2] + '(?:["(])'));
            cantidad++;
        }
    }
    assert.ok(cantidad > 50);
});
test('los detalles del carrito escapan etiquetas y atributos introducidos por URL', () => {
    const context = vm.createContext({ document: { addEventListener() {} } });
    vm.runInContext(fs.readFileSync(path.join(recursos, 'static/js/cart.js'), 'utf8'), context);
    const html = context.formatearDetallesComoLista('(<img src=x onerror=evil>)');
    assert.doesNotMatch(html, /<img/);
    assert.match(html, /&lt;img/);
    assert.equal(context.escaparHtml('" onerror="evil'), '&quot; onerror=&quot;evil');
});

function entorno() {
    const memoria = new Map([
        ['carrito_caleta', JSON.stringify([{ productoId: 7, cantidad: 2, precio: 0.01 }])],
        ['pedido_envio_caleta', JSON.stringify({ tipoEntrega: 'delivery', direccion: 'Av. Lima 123' })]
    ]);
    const intentos = new Map();
    const storage = mapa => ({ getItem: key => mapa.get(key) ?? null, setItem: (k, v) => mapa.set(k, v), removeItem: k => mapa.delete(k) });
    const boton = { disabled: false, textContent: 'Confirmar pedido', dataset: { email: 'cliente@example.com' } };
    const modales = [];
    const requests = [];
    let fallo = null;
    const context = vm.createContext({
        document: { addEventListener() {}, getElementById: () => boton, querySelector: () => ({ value: 'EFECTIVO' }) },
        localStorage: storage(memoria), sessionStorage: storage(intentos),
        obtenerCarrito: () => JSON.parse(memoria.get('carrito_caleta') || '[]'),
        crypto: { randomUUID: () => 'a1234567-1234-1234-1234-123456789abc' },
        mostrarModalMensaje: (...args) => modales.push(args), window: { location: {} },
        fetch: async (url, options) => {
            requests.push({ url, datos: JSON.parse(options.body) });
            if (fallo === 'red') throw new TypeError('sin conexión');
            return { ok: !fallo, json: async () => fallo ? { error: 'Stock insuficiente' } : { pedidoId: 42, total: 55 } };
        }, TypeError
    });
    vm.runInContext(fs.readFileSync(path.join(recursos, 'static/js/payment.js'), 'utf8'), context);
    return { context, memoria, intentos, boton, modales, requests, fallar: valor => { fallo = valor; }, comprar: () => context.procesarPagoFinal({ preventDefault() {} }) };
}
test('el pago llama al backend sin precios y limpia el carrito solo tras confirmar', async () => {
    const e = entorno();
    await e.comprar();
    assert.equal(e.requests[0].url, '/api/checkout');
    assert.deepEqual(e.requests[0].datos.items, [{ productoId: 7, cantidad: 2 }]);
    assert.equal(e.requests[0].datos.metodoPago, 'CONTRA_ENTREGA');
    assert.equal(e.requests[0].datos.direccionEntrega, 'Av. Lima 123');
    assert.equal(e.memoria.has('carrito_caleta'), false);
    assert.equal(e.intentos.size, 0);
    assert.match(e.modales[0][1], /pendiente de envío/);
});
test('respuesta de error conserva el carrito y permite reintentar', async () => {
    const e = entorno(); e.fallar('stock');
    await e.comprar();
    assert.ok(e.memoria.has('carrito_caleta'));
    assert.equal(e.boton.disabled, false);
    assert.match(e.modales[0][1], /Stock insuficiente/);
});
test('retiro envía pago en tienda y no depende de una pasarela', async () => {
    const e = entorno();
    e.memoria.set('pedido_envio_caleta', JSON.stringify({ tipoEntrega: 'retiro' }));
    await e.comprar();
    assert.equal(e.requests[0].datos.tipoEntrega, 'RETIRO');
    assert.equal(e.requests[0].datos.metodoPago, 'PAGO_EN_TIENDA');
    assert.equal(e.requests[0].datos.direccionEntrega, null);
});
test('error de red reutiliza la misma clave al reintentar', async () => {
    const e = entorno(); e.fallar('red');
    await e.comprar();
    assert.ok(e.memoria.has('carrito_caleta'));
    e.fallar(null);
    await e.comprar();
    assert.equal(e.requests[0].datos.claveCompra, e.requests[1].datos.claveCompra);
    assert.equal(e.memoria.has('carrito_caleta'), false);
});
