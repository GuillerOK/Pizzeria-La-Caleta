document.addEventListener("DOMContentLoaded", () => {
    cargarResumenPago();
    cargarDatosEnvioConfirmados();
});

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
            if (labelDescuento) labelDescuento.innerHTML = `<i class="fa-solid fa-tag me-1"></i>${cupon.titulo || 'Cupón'}`;
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
        if (metodo) metodo.textContent = datosEnvio.tipoEntrega === 'delivery' ? 'Pago contra entrega' : 'Pago al retirar en tienda';
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
            document.getElementById('pago-destino-tiempo').innerText = "Listo en: 20 - 30 min";
        }
    }
}

async function procesarPagoFinal(e) {
    e.preventDefault();
    const btn = document.getElementById('btnFinalizarPedido');
    if (btn.disabled) return;
    const texto = btn.textContent;
    btn.disabled = true;
    btn.textContent = 'Registrando pedido...';
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
            metodoPago: envio.tipoEntrega === 'delivery' ? 'CONTRA_ENTREGA' : 'PAGO_EN_TIENDA',
            codigoCupon: cupon ? cupon.codigo : null
        };
        // Mantener la clave al reintentar tras perder una respuesta del servidor.
        const firma = JSON.stringify(datos);
        let intento = JSON.parse(sessionStorage.getItem('checkout_intento') || 'null');
        if (!intento || intento.firma !== firma) {
            intento = { firma, clave: crypto.randomUUID() };
            sessionStorage.setItem('checkout_intento', JSON.stringify(intento));
        }
        const respuesta = await fetch('/api/checkout', {
            method: 'POST', credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ ...datos, claveCompra: intento.clave })
        });
        const resultado = await respuesta.json().catch(() => ({}));
        if (!respuesta.ok) throw new Error(resultado.error || 'No se pudo registrar la compra.');
        localStorage.removeItem('carrito_caleta');
        localStorage.removeItem('pedido_envio_caleta');
        localStorage.removeItem('cupon_caleta');
        sessionStorage.removeItem('checkout_intento');
        mostrarModalMensaje('Pedido registrado', 'Pedido #' + resultado.pedidoId +
            '. Tu boleta fue enviada al correo. ' + (datos.tipoEntrega === 'DELIVERY' ? 'Paga al recibir tu pedido.' : 'Paga al retirar tu pedido en tienda.'),
            () => { window.location.href = '/perfil?tab=pedidos'; });
    } catch (error) {
        mostrarModalMensaje('Error de compra', error instanceof TypeError
            ? 'No se pudo confirmar la respuesta. Revisa tu historial o reintenta con el mismo carrito.' : error.message);
        btn.disabled = false;
        btn.textContent = texto;
    }
}
