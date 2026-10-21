document.addEventListener("DOMContentLoaded", () => {
    cargarResumenPago();
    cargarDatosEnvioConfirmados();
    inicializarPasarela();
});

function erroresTarjeta(datos, ahora = new Date()) {
    const errores = {};
    if (!/^\d{16}$/.test(datos.numero.replace(/\s/g, ''))) errores.numeroTarjeta = 'Ingresa los 16 dígitos de la tarjeta de prueba.';
    if (datos.titular.trim().length < 2) errores.titularTarjeta = 'Ingresa el nombre del titular.';
    const fecha = /^(0[1-9]|1[0-2])\/(\d{2})$/.exec(datos.expiracion);
    if (!fecha) errores.expiracionTarjeta = 'Usa el formato MM/YY, con un mes entre 01 y 12.';
    else if (2000 + Number(fecha[2]) < ahora.getFullYear() ||
        (2000 + Number(fecha[2]) === ahora.getFullYear() && Number(fecha[1]) < ahora.getMonth() + 1)) {
        errores.expiracionTarjeta = 'La fecha de expiración ya pasó.';
    }
    if (!/^\d{3,4}$/.test(datos.cvv)) errores.cvvTarjeta = 'El CVV debe tener 3 o 4 dígitos.';
    return errores;
}

function validarTarjeta() {
    const ids = ['numeroTarjeta', 'titularTarjeta', 'expiracionTarjeta', 'cvvTarjeta'];
    const errores = erroresTarjeta({ numero: document.getElementById(ids[0]).value,
        titular: document.getElementById(ids[1]).value, expiracion: document.getElementById(ids[2]).value,
        cvv: document.getElementById(ids[3]).value });
    ids.forEach(id => {
        document.getElementById(id).setAttribute('aria-invalid', errores[id] ? 'true' : 'false');
        document.getElementById('error-' + id).textContent = errores[id] || '';
    });
    const primero = ids.find(id => errores[id]);
    if (primero) {
        document.getElementById(primero).focus();
        document.getElementById('estadoPasarela').textContent = 'Revisa los campos marcados antes de pagar.';
    }
    return !primero;
}

function inicializarPasarela() {
    const numero = document.getElementById('numeroTarjeta');
    const fecha = document.getElementById('expiracionTarjeta');
    numero.addEventListener('input', () => { numero.value = numero.value.replace(/\D/g, '').slice(0, 16).replace(/(.{4})/g, '$1 ').trim(); });
    fecha.addEventListener('input', () => { const n = fecha.value.replace(/\D/g, '').slice(0, 4); fecha.value = n.length > 2 ? n.slice(0, 2) + '/' + n.slice(2) : n; });
    document.getElementById('btnDatosPrueba').addEventListener('click', () => {
        numero.value = '4242 4242 4242 4242';
        document.getElementById('titularTarjeta').value = 'CLIENTE DEMO';
        fecha.value = '12/' + String(new Date().getFullYear() + 2).slice(-2);
        document.getElementById('cvvTarjeta').value = '123';
        validarTarjeta();
        document.getElementById('estadoPasarela').textContent = 'Datos de prueba listos. Puedes continuar con el pago simulado.';
    });
}

function estadoProcesando(procesando) {
    document.getElementById('btnFinalizarPedido').disabled = procesando;
    document.getElementById('camposPago').disabled = procesando;
    document.getElementById('btnDatosPrueba').disabled = procesando;
    document.getElementById('formCheckoutPago').setAttribute('aria-busy', String(procesando));
    document.getElementById('spinnerPago').classList.toggle('d-none', !procesando);
    document.getElementById('textoBotonPago').textContent = procesando ? 'Procesando pago...' : 'Pagar';
}

function cargarResumenPago() {
    const carrito = JSON.parse(localStorage.getItem('carrito_caleta')) || [];
    const contenedorLista = document.getElementById('resumen-lista-pago');
    const badgeCant = document.getElementById('resumen-cant-articulos');
    const subtotalDOM = document.getElementById('pago-subtotal');
    const envioDOM = document.getElementById('pago-envio-costo');
    const totalDOM = document.getElementById('pago-total');

    if (carrito.length === 0) {
        window.location.href = '/menu';
        return;
    }

    let subtotal = 0;
    let cantidadTotal = 0;

    contenedorLista.innerHTML = carrito.map(item => {
        subtotal += item.precio * item.cantidad;
        cantidadTotal += item.cantidad;

        const pos = item.nombre.indexOf('(');
        const nombreSimple = pos !== -1 ? item.nombre.substring(0, pos).trim() : item.nombre;

        return `
            <div class="d-flex align-items-center justify-content-between gap-2">
                <div class="d-flex align-items-center gap-2 min-w-0">
                    <img src="${escaparHtml(item.imagenUrl)}" alt="${escaparHtml(nombreSimple)}" class="rounded-2 object-fit-cover flex-shrink-0" style="width: 44px; height: 44px;">
                    <div class="min-w-0">
                        <h6 class="fw-bold mb-0 text-white small text-truncate" style="max-width: 170px;">${escaparHtml(nombreSimple)}</h6>
                        <span class="text-secondary d-block" style="font-size: 11px;">Cant: ${item.cantidad}</span>
                    </div>
                </div>
                <span class="fw-bold text-white small flex-shrink-0">S/ ${(item.precio * item.cantidad).toFixed(2)}</span>
            </div>
        `;
    }).join('');

    const datosEnvio = JSON.parse(localStorage.getItem('pedido_envio_caleta')) || {};
    const costoEnvio = datosEnvio.tipoEntrega === 'delivery' ? 5.00 : 0.00;
    const cupon = JSON.parse(localStorage.getItem('cupon_caleta') || 'null');
    let descuento = 0;
    const filaDescuento = document.getElementById('pago-fila-descuento');
    const labelDescuento = document.getElementById('pago-descuento-label');
    const valorDescuentoDOM = document.getElementById('pago-descuento');

    if (cupon && subtotal > 0) {
        if (cupon.tipo === 'REGALO') {
            descuento = 0;
        } else if (cupon.tipo === 'DELIVERY_GRATIS') {
            descuento = costoEnvio;
        } else if (cupon.tipo === 'PORCENTAJE') {
            descuento = Math.round((subtotal * ((cupon.valorDescuento || cupon.descuento) / 100.0)) * 100) / 100;
        } else {
            descuento = Math.min(subtotal, cupon.valorDescuento ?? cupon.descuento ?? 0);
        }

        if (filaDescuento) {
            filaDescuento.classList.remove('d-none');
            if (valorDescuentoDOM) valorDescuentoDOM.innerText = cupon.tipo === 'REGALO' ? `${cupon.titulo} (CUPÓN REGALO) — S/ 0.00` : `- S/ ${descuento.toFixed(2)}`;
            if (labelDescuento) labelDescuento.innerHTML = `<i class="fa-solid fa-tag me-1"></i>${escaparHtml(cupon.titulo || 'Cupón')}`;
        }
    } else {
        if (filaDescuento) filaDescuento.classList.add('d-none');
    }

    const total = Math.max(0, subtotal + costoEnvio - descuento);

    if (badgeCant) badgeCant.innerText = cantidadTotal;
    if (subtotalDOM) subtotalDOM.innerText = `S/ ${subtotal.toFixed(2)}`;
    if (envioDOM) {
        envioDOM.innerText = costoEnvio > 0 ? `S/ ${costoEnvio.toFixed(2)}` : "Gratis (Retiro en Tienda)";
        envioDOM.className = costoEnvio > 0 ? "text-white fw-semibold" : "text-success fw-semibold";
    }
    if (totalDOM) totalDOM.innerText = `S/ ${total.toFixed(2)}`;
}

function cargarDatosEnvioConfirmados() {
    const datosEnvio = JSON.parse(localStorage.getItem('pedido_envio_caleta'));
    
    if (datosEnvio) {
        const metodo = document.getElementById('textoMetodoPago');
        if (metodo) metodo.textContent = 'Pago online simulado';
        if (datosEnvio.nombre) document.getElementById('pago-contacto-nombre').innerText = datosEnvio.nombre;
        if (datosEnvio.correo) document.getElementById('pago-contacto-correo').innerText = datosEnvio.correo;
        if (datosEnvio.telefono) document.getElementById('pago-contacto-telefono').innerText = datosEnvio.telefono;

        if (datosEnvio.tipoEntrega === 'delivery') {
            document.getElementById('pago-titulo-metodo').innerText = "Dirección de Entrega (Delivery):";
            document.getElementById('pago-destino-principal').innerText = datosEnvio.direccion || "Dirección registrada";
            document.getElementById('pago-destino-detalle').innerText = datosEnvio.referencia || "Sin referencias adicionales";
            document.getElementById('pago-destino-tiempo').innerText = "Tiempo estimado: 35 - 50 min";
        } else {
            document.getElementById('pago-titulo-metodo').innerText = "Punto de Retiro Seleccionado:";
            document.getElementById('pago-destino-principal').innerText = datosEnvio.sede || "Sede Miraflores";
            document.getElementById('pago-destino-detalle').innerText = datosEnvio.direccionSede || "Av. José Larco 850, Miraflores";
            document.getElementById('pago-destino-tiempo').innerText = "Recojo habilitado tras pagar. Preparación: 20 - 30 min";
        }
    }
}

async function procesarPagoFinal(e) {
    e.preventDefault();
    const btn = document.getElementById('btnFinalizarPedido');
    if (btn.disabled) return;
    if (!validarTarjeta()) return;
    estadoProcesando(true);
    document.getElementById('estadoPasarela').textContent = 'Conectando con el banco de demostración…';
    try {
        const carrito = obtenerCarrito();
        const envio = JSON.parse(localStorage.getItem('pedido_envio_caleta') || '{}');
        if (!btn.dataset.email) throw new Error('Inicia sesión para comprar.');
        if (!carrito.length || carrito.some(i => !Number.isInteger(i.productoId) || i.productoId <= 0)) {
            throw new Error('Actualiza tu carrito desde el menú antes de comprar.');
        }
        if (!envio.tipoEntrega) throw new Error('Completa los datos de envío antes de comprar.');
        const cupon = JSON.parse(localStorage.getItem('cupon_caleta') || 'null');
        const datos = {
            email: btn.dataset.email,
            items: carrito.map(i => ({ productoId: i.productoId, cantidad: i.cantidad })),
            tipoEntrega: envio.tipoEntrega === 'delivery' ? 'DELIVERY' : 'RETIRO',
            direccionEntrega: envio.direccion || null,
            metodoPago: 'ONLINE_SIMULADO',
            codigoCupon: cupon ? cupon.codigo : null
        };
        // Mantener la clave al reintentar tras perder una respuesta del servidor.
        const firma = JSON.stringify(datos);
        let intento = JSON.parse(sessionStorage.getItem('checkout_intento') || 'null');
        if (!intento || intento.firma !== firma) {
            intento = { firma, clave: crypto.randomUUID() };
            sessionStorage.setItem('checkout_intento', JSON.stringify(intento));
        }
        // Pausa demostrativa: no se contacta a bancos ni se transmiten datos de tarjeta.
        await new Promise(resolve => setTimeout(resolve, 2000));
        document.getElementById('estadoPasarela').textContent = 'Simulación aprobada. Confirmando tu pedido…';
        const respuesta = await fetch('/api/checkout', {
            method: 'POST', credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ ...datos, claveCompra: intento.clave })
        });
        const resultado = await respuesta.json().catch(() => ({}));
        if (!respuesta.ok) throw new Error(resultado.error || 'No se pudo registrar la compra.');
        if (!Number.isInteger(resultado.pedidoId) || !['REALIZADO', 'ENTREGADO'].includes(resultado.estado)) {
            throw new Error('El servidor no confirmó el pago. Revisa tu historial antes de reintentar.');
        }
        localStorage.removeItem('carrito_caleta');
        localStorage.removeItem('pedido_envio_caleta');
        localStorage.removeItem('cupon_caleta');
        localStorage.removeItem('cupon_precargado_caleta');
        sessionStorage.removeItem('checkout_intento');
        document.getElementById('formCheckoutPago').reset();
        window.location.href = '/pago/exitoso?pedidoId=' + resultado.pedidoId;
    } catch (error) {
        mostrarModalMensaje('Error de compra', error instanceof TypeError
            ? 'No se pudo confirmar la respuesta. Revisa tu historial o reintenta con el mismo carrito.' : error.message);
        estadoProcesando(false);
        document.getElementById('estadoPasarela').textContent = 'No se confirmó la compra. Tu carrito y cupón siguen disponibles.';
    }
}
