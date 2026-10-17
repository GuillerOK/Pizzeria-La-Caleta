document.addEventListener("DOMContentLoaded", () => {
    cargarResumenEnvio();
    verificarEstadoUsuario();
});

let subtotalPedido = 0;

// 1. Alterna entre vista Invitado y Usuario Logeado
function verificarEstadoUsuario() {
    // Revisa usuario guardado en localStorage (o tu clave de auth.js ej: 'usuario_caleta', 'usuario_actual')
    const datos = document.getElementById("usuarioCheckout")?.dataset;
    const usuarioLogeado = datos ? { ...datos, documento: datos.dni } : null;
    const seccionInvitado = document.getElementById('seccion-invitado');
    const seccionLogueado = document.getElementById('seccion-logueado');

    if (usuarioLogeado) {
        // Está autenticado -> Mostramos formulario completo editable
        seccionInvitado.classList.add('d-none');
        seccionLogueado.classList.remove('d-none');

        // Precarga sus datos personales registrados
        if (usuarioLogeado.nombre) document.getElementById('inputNombreUser').value = usuarioLogeado.nombre;
        if (usuarioLogeado.correo || usuarioLogeado.email) document.getElementById('inputCorreoUser').value = usuarioLogeado.correo || usuarioLogeado.email;
        if (usuarioLogeado.telefono) document.getElementById('inputTelefonoUser').value = usuarioLogeado.telefono;
        if (usuarioLogeado.documento) document.getElementById('inputDocumentoUser').value = usuarioLogeado.documento;
        if (usuarioLogeado.direccion) document.getElementById('inputDireccionUser').value = usuarioLogeado.direccion;
        if (usuarioLogeado.referencia) document.getElementById('inputReferenciaUser').value = usuarioLogeado.referencia;
    } else {
        // No está logeado -> Solo pedimos correo
        seccionInvitado.classList.remove('d-none');
        seccionLogueado.classList.add('d-none');

        // Si ya había escrito un correo antes en esta sesión, lo precarga
        const previo = JSON.parse(localStorage.getItem('pedido_envio_caleta'));
        if (previo && previo.correo) {
            document.getElementById('inputCorreoInvitado').value = previo.correo;
        }
    }
}

// 2. Control de Modalidad: Retiro vs. Delivery
function cambiarModalidadEntrega(tipo) {
    const camposDelivInvitado = document.getElementById('campos-delivery-invitado');
    const inputDirInvitado = document.getElementById('inputDireccionInvitado');

    const camposDelivUser = document.getElementById('campos-delivery-user');
    const inputDirUser = document.getElementById('inputDireccionUser');

    const cardSede = document.getElementById('card-sede-info');

    if (tipo === 'delivery') {
        camposDelivInvitado.classList.remove('d-none');
        inputDirInvitado.setAttribute('required', 'true');

        camposDelivUser.classList.remove('d-none');
        inputDirUser.setAttribute('required', 'true');

        if (cardSede) cardSede.classList.add('d-none');
    } else {
        camposDelivInvitado.classList.add('d-none');
        inputDirInvitado.removeAttribute('required');

        camposDelivUser.classList.add('d-none');
        inputDirUser.removeAttribute('required');

        if (cardSede) cardSede.classList.remove('d-none');
    }

    actualizarTotalesEnvio(tipo);
}

function actualizarTotalesEnvio(tipo) {
    const costoEnvioDOM = document.getElementById('shipping-costo-envio');
    const totalDOM = document.getElementById('shipping-total');

    const costoEnvio = tipo === 'delivery' ? 5.00 : 0.00;
    const total = subtotalPedido + costoEnvio;

    if (costoEnvioDOM) {
        costoEnvioDOM.innerText = costoEnvio > 0 ? `S/ ${costoEnvio.toFixed(2)}` : "Gratis (Retiro)";
        costoEnvioDOM.className = costoEnvio > 0 ? "text-white fw-semibold" : "text-success fw-semibold";
    }
    if (totalDOM) totalDOM.innerText = `S/ ${total.toFixed(2)}`;
}

// 3. Cargar Carrito Real
function cargarResumenEnvio() {
    const carrito = JSON.parse(localStorage.getItem('carrito_caleta')) || [];
    const contenedorLista = document.getElementById('shipping-lista-productos');
    const badgeCant = document.getElementById('shipping-cant-articulos');
    const subtotalDOM = document.getElementById('shipping-subtotal');

    if (carrito.length === 0) {
        window.location.href = '/menu';
        return;
    }

    subtotalPedido = 0;
    let cantidadTotal = 0;

    contenedorLista.innerHTML = carrito.map(item => {
        subtotalPedido += item.precio * item.cantidad;
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

    if (badgeCant) badgeCant.innerText = cantidadTotal;
    if (subtotalDOM) subtotalDOM.innerText = `S/ ${subtotalPedido.toFixed(2)}`;

    const modalidadActual = document.querySelector('input[name="tipoModalidad"]:checked').value;
    actualizarTotalesEnvio(modalidadActual);
}

// 4. Procesa envío para Invitado
function procesarPasoEnvioInvitado(e) {
    e.preventDefault();
    const tipoModalidad = document.querySelector('input[name="tipoModalidad"]:checked').value;

    const datosPedido = {
        esInvitado: true,
        nombre: "Cliente Invitado",
        correo: document.getElementById('inputCorreoInvitado').value.trim(),
        telefono: "Por confirmar",
        documento: "-",
        tipoEntrega: tipoModalidad,
        sede: "Sede Miraflores",
        direccionSede: "Av. José Larco 850, Miraflores, Lima",
        direccion: tipoModalidad === 'delivery' ? document.getElementById('inputDireccionInvitado').value.trim() : "",
        referencia: tipoModalidad === 'delivery' ? document.getElementById('inputReferenciaInvitado').value.trim() : ""
    };

    localStorage.setItem('pedido_envio_caleta', JSON.stringify(datosPedido));
    window.location.href = '/pago';
}

// 5. Procesa envío para Usuario Logeado
function procesarPasoEnvioLogueado(e) {
    e.preventDefault();
    const tipoModalidad = document.querySelector('input[name="tipoModalidad"]:checked').value;

    const datosPedido = {
        esInvitado: false,
        nombre: document.getElementById('inputNombreUser').value.trim(),
        correo: document.getElementById('inputCorreoUser').value.trim(),
        telefono: document.getElementById('inputTelefonoUser').value.trim(),
        documento: document.getElementById('inputDocumentoUser').value.trim(),
        tipoEntrega: tipoModalidad,
        sede: "Sede Miraflores",
        direccionSede: "Av. José Larco 850, Miraflores, Lima",
        direccion: tipoModalidad === 'delivery' ? document.getElementById('inputDireccionUser').value.trim() : "",
        referencia: tipoModalidad === 'delivery' ? document.getElementById('inputReferenciaUser').value.trim() : ""
    };

    localStorage.setItem('pedido_envio_caleta', JSON.stringify(datosPedido));
    window.location.href = '/pago';
}