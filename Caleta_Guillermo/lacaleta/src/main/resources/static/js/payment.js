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
                    <img src="${item.imagenUrl}" alt="${nombreSimple}" class="rounded-2 object-fit-cover flex-shrink-0" style="width: 44px; height: 44px;">
                    <div class="min-w-0">
                        <h6 class="fw-bold mb-0 text-white small text-truncate" style="max-width: 170px;">${nombreSimple}</h6>
                        <span class="text-secondary d-block" style="font-size: 11px;">Cant: ${item.cantidad}</span>
                    </div>
                </div>
                <span class="fw-bold text-white small flex-shrink-0">S/ ${(item.precio * item.cantidad).toFixed(2)}</span>
            </div>
        `;
    }).join('');

    const datosEnvio = JSON.parse(localStorage.getItem('pedido_envio_caleta')) || {};
    const costoEnvio = datosEnvio.tipoEntrega === 'delivery' ? 5.00 : 0.00;
    const total = subtotal + costoEnvio;

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

function alternarCamposFactura(mostrar) {
    const box = document.getElementById('campos-factura');
    const ruc = document.getElementById('factura-ruc');
    const razon = document.getElementById('factura-razon');
    
    if (mostrar) {
        box.classList.remove('d-none');
        ruc.setAttribute('required', 'true');
        razon.setAttribute('required', 'true');
    } else {
        box.classList.add('d-none');
        ruc.removeAttribute('required');
        razon.removeAttribute('required');
    }
}

function alternarDetalleMetodoPago(metodo) {
    const boxTarjeta = document.getElementById('box-formulario-tarjeta');
    const boxYape = document.getElementById('box-formulario-yape');
    const wrapTarjeta = document.getElementById('wrapper-tarjeta');
    const wrapYape = document.getElementById('wrapper-yape');

    if (metodo === 'tarjeta') {
        boxTarjeta.classList.remove('d-none');
        boxYape.classList.add('d-none');
        wrapTarjeta.classList.add('activo');
        wrapYape.classList.remove('activo');
    } else {
        boxTarjeta.classList.add('d-none');
        boxYape.classList.remove('d-none');
        wrapTarjeta.classList.remove('activo');
        wrapYape.classList.add('activo');
    }
}

function procesarPagoFinal(e) {
    e.preventDefault();

    const btn = document.getElementById('btnFinalizarPedido');
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2"></i> Procesando pedido...';

    setTimeout(() => {
        localStorage.removeItem('carrito_caleta');
        localStorage.removeItem('pedido_envio_caleta');
        window.location.href = '/pedido-confirmado'; 
    }, 1800);
}