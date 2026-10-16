const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

// Ejecuta los listeners reales de auth.js con DOM, fetch y Bootstrap controlados.
function entorno(pagina = 'registro') {
    const elementos = new Map();
    function elemento(id, value = '') {
        const listeners = new Map();
        const classes = new Set();
        const el = {
            value, textContent: '', innerText: '', tagName: 'BUTTON', disabled: false, dataset: {},
            classList: { add: c => classes.add(c), remove: c => classes.delete(c), contains: c => classes.has(c) },
            addEventListener(event, fn, options) {
                const list = listeners.get(event) || [];
                list.push({ fn, once: options?.once });
                listeners.set(event, list);
            },
            removeEventListener(event, fn) {
                listeners.set(event, (listeners.get(event) || []).filter(item => item.fn !== fn));
            },
            async emitir(event) {
                for (const item of [...(listeners.get(event) || [])]) {
                    if (item.once) this.removeEventListener(event, item.fn);
                    await item.fn({ preventDefault() {} });
                }
            },
            querySelector: () => elementos.get(id + 'Boton'),
            reset() {}, focus() {}, reportValidity: () => true
        };
        elementos.set(id, el);
        return el;
    }
    for (const id of ['formRegistro', 'formRegistroBoton', 'formVerificacionCuenta',
        'formVerificacionCuentaBoton', 'modalVerificacionCuenta', 'otpVerificacionCuenta',
        'correoVerificacionCuenta', 'errorVerificacionCuenta', 'modalNotificacionGlobal',
        'modalNotificacionGlobalLabel', 'modalNotificacionGlobalMensaje']) elemento(id);
    const campos = { regNombre: 'Camila', regApellido: 'Torres', regTipoDoc: 'DNI', regNumDoc: '72839401',
        regTelefono: '987654321', regFechaNac: '2000-01-01', regEmail: 'cliente@example.com',
        regPassword: 'ClaveSegura1!', regPasswordConfirm: 'ClaveSegura1!' };
    for (const [id, value] of Object.entries(campos)) elemento(id, value);
    elemento('checkTerminos').checked = true;
    if (pagina !== 'registro') {
        for (const id of ['formForgotPassword', 'formVerifyCode', 'formResetPassword']) {
            elemento(id);
            elemento(id + 'Boton').tagName = 'INPUT';
        }
        elemento('forgotEmail', ' CLIENTE@example.com ');
        elemento('otpCode', '012345');
        elemento('newPassword', 'NuevaClave1!');
        elemento('confirmPassword', 'NuevaClave1!');
        elemento('reenviarCodigoRecuperacion');
    }
    const document = elemento('document');
    document.getElementById = id => elementos.get(id) || null;
    document.querySelectorAll = () => [];
    const modales = new Map();
    const llamadas = [];
    const respuestas = [];
    const window = { location: { protocol: 'http:', pathname: '/' + pagina, href: '/' + pagina } };
    const sesion = new Map();
    const local = new Map();
    const storage = datos => ({ getItem: key => datos.get(key) || null,
        setItem: (key, value) => datos.set(key, value), removeItem: key => datos.delete(key) });
    const sandbox = {
        document, window, localStorage: storage(local), sessionStorage: storage(sesion),
        bootstrap: { Modal: { getOrCreateInstance(el) {
            if (!modales.has(el)) modales.set(el, {
                visible: false,
                show() { this.visible = true; },
                hide() { this.visible = false; el.emitir('hidden.bs.modal'); }
            });
            return modales.get(el);
        } } },
        async fetch(url, options) {
            llamadas.push({ url, body: JSON.parse(options.body) });
            const respuesta = await respuestas.shift();
            if (respuesta instanceof Error) throw respuesta;
            return { ok: respuesta.ok, json: async () => respuesta.data };
        }
    };
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/auth.js'), 'utf8'), sandbox);
    return { elementos, document, window, llamadas, respuestas, modales, sesion, local };
}

test('registro abre OTP sin redirigir; verificación redirige solo al cerrar bienvenida', async () => {
    const e = entorno();
    await e.document.emitir('DOMContentLoaded');
    e.respuestas.push({ ok: true, data: { email: 'cliente@example.com' } });
    await e.elementos.get('formRegistro').emitir('submit');
    assert.equal(e.llamadas[0].url, '/api/auth/register');
    assert.equal(e.modales.get(e.elementos.get('modalVerificacionCuenta')).visible, true);
    assert.equal(e.window.location.href, '/registro');
    e.elementos.get('otpVerificacionCuenta').value = '012345';
    e.respuestas.push({ ok: true, data: { mensaje: 'Bienvenido' } });
    await e.elementos.get('formVerificacionCuenta').emitir('submit');
    assert.equal(e.llamadas[1].url, '/api/auth/verify-account');
    assert.equal(e.llamadas[1].body.otp, '012345');
    assert.equal(e.modales.get(e.elementos.get('modalNotificacionGlobal')).visible, true);
    assert.equal(e.window.location.href, '/registro');
    await e.elementos.get('modalNotificacionGlobal').emitir('hidden.bs.modal');
    assert.equal(e.window.location.href, '/login');
});

test('bloquea doble registro durante fetch y restablece botón tras error de red', async () => {
    const e = entorno();
    await e.document.emitir('DOMContentLoaded');
    let resolver;
    e.respuestas.push(new Promise(resolve => { resolver = resolve; }));
    const envio = e.elementos.get('formRegistro').emitir('submit');
    assert.equal(e.elementos.get('formRegistroBoton').disabled, true);
    await e.elementos.get('formRegistro').emitir('submit');
    assert.equal(e.llamadas.length, 1);
    resolver(new Error('sin conexión'));
    await envio;
    assert.equal(e.elementos.get('formRegistroBoton').disabled, false);
    assert.equal(e.modales.get(e.elementos.get('modalNotificacionGlobal')).visible, true);
    assert.equal(e.window.location.href, '/registro');
});

test('OTP rechazado mantiene modal abierto y permite corregirlo', async () => {
    const e = entorno();
    await e.document.emitir('DOMContentLoaded');
    e.respuestas.push({ ok: true, data: { email: 'cliente@example.com' } });
    await e.elementos.get('formRegistro').emitir('submit');
    e.elementos.get('otpVerificacionCuenta').value = '999999';
    e.respuestas.push({ ok: false, data: { error: 'Código incorrecto' } });
    await e.elementos.get('formVerificacionCuenta').emitir('submit');
    assert.equal(e.elementos.get('errorVerificacionCuenta').textContent, 'Código incorrecto');
    assert.equal(e.modales.get(e.elementos.get('modalVerificacionCuenta')).visible, true);
    assert.equal(e.elementos.get('formVerificacionCuentaBoton').disabled, false);
    assert.equal(e.window.location.href, '/registro');
});

test('recuperación espera envío real, verifica con API y persiste contraseña antes de mostrar éxito', async () => {
    const e = entorno('forgotpassword');
    await e.document.emitir('DOMContentLoaded');
    let resolver;
    e.respuestas.push(new Promise(resolve => { resolver = resolve; }));
    const solicitud = e.elementos.get('formForgotPassword').emitir('submit');
    assert.equal(e.elementos.get('formForgotPasswordBoton').disabled, true);
    assert.equal(e.modales.size, 0);
    await e.elementos.get('formForgotPassword').emitir('submit');
    assert.equal(e.llamadas.length, 1);
    assert.equal(e.llamadas[0].url, '/api/auth/forgot-password');
    assert.equal(e.llamadas[0].body.email, 'cliente@example.com');
    resolver({ ok: true, data: { mensaje: 'Código enviado por correo.' } });
    await solicitud;
    assert.equal(e.elementos.get('formForgotPasswordBoton').disabled, false);
    assert.equal(e.sesion.get('reset_email'), 'cliente@example.com');
    assert.equal(e.sesion.has('reset_otp'), false);
    assert.doesNotMatch(e.elementos.get('modalNotificacionGlobalMensaje').innerText, /simulado|123456/i);
    await e.elementos.get('modalNotificacionGlobal').emitir('hidden.bs.modal');
    assert.equal(e.window.location.href, '/verifycode');
    e.respuestas.push({ ok: true, data: { mensaje: 'Código correcto.' } });
    await e.elementos.get('formVerifyCode').emitir('submit');
    assert.equal(e.llamadas[1].url, '/api/auth/verify-otp');
    assert.equal(e.llamadas[1].body.otp, '012345');
    assert.equal(e.sesion.get('reset_otp'), '012345');
    await e.elementos.get('modalNotificacionGlobal').emitir('hidden.bs.modal');
    assert.equal(e.window.location.href, '/resetpassword');
    e.respuestas.push({ ok: true, data: { mensaje: 'Contraseña actualizada en BD.' } });
    await e.elementos.get('formResetPassword').emitir('submit');
    assert.equal(e.llamadas[2].url, '/api/auth/reset-password');
    assert.equal(e.llamadas[2].body.newPassword, 'NuevaClave1!');
    assert.equal(e.llamadas[2].body.otp, '012345');
    assert.equal(e.sesion.size, 0);
    assert.equal(e.window.location.href, '/resetpassword');
    await e.elementos.get('modalNotificacionGlobal').emitir('hidden.bs.modal');
    assert.equal(e.window.location.href, '/login');
});

test('fallo SMTP se muestra como error sin anunciar envío ni redirigir', async () => {
    const e = entorno('forgotpassword');
    await e.document.emitir('DOMContentLoaded');
    e.respuestas.push({ ok: false, data: { error: 'Error al enviar el correo.' } });
    await e.elementos.get('formForgotPassword').emitir('submit');
    assert.equal(e.elementos.get('modalNotificacionGlobalMensaje').innerText, 'Error al enviar el correo.');
    assert.equal(e.sesion.size, 0);
    assert.equal(e.elementos.get('formForgotPasswordBoton').disabled, false);
    await e.elementos.get('modalNotificacionGlobal').emitir('hidden.bs.modal');
    assert.equal(e.window.location.href, '/forgotpassword');
});

test('un antiguo código local nunca autoriza el cambio si el servidor lo rechaza', async () => {
    const e = entorno('verifycode');
    e.local.set('temp_otp_code', '123456');
    e.sesion.set('reset_email', 'cliente@example.com');
    e.elementos.get('otpCode').value = '123456';
    await e.document.emitir('DOMContentLoaded');
    e.respuestas.push({ ok: false, data: { error: 'Código incorrecto o vencido.' } });
    await e.elementos.get('formVerifyCode').emitir('submit');
    assert.equal(e.llamadas[0].url, '/api/auth/verify-otp');
    assert.equal(e.sesion.has('reset_otp'), false);
    await e.elementos.get('modalNotificacionGlobal').emitir('hidden.bs.modal');
    assert.equal(e.window.location.href, '/verifycode');
});

test('reenviar código llama SMTP por API y borra la verificación anterior', async () => {
    const e = entorno('verifycode');
    e.sesion.set('reset_email', 'cliente@example.com');
    e.sesion.set('reset_otp', '012345');
    await e.document.emitir('DOMContentLoaded');
    e.respuestas.push({ ok: true, data: { mensaje: 'Código reenviado.' } });
    await e.elementos.get('reenviarCodigoRecuperacion').emitir('click');
    assert.equal(e.llamadas[0].url, '/api/auth/forgot-password');
    assert.equal(e.sesion.has('reset_otp'), false);
});
