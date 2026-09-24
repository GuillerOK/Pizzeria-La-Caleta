function obtenerCarrito() {
    return JSON.parse(localStorage.getItem('carrito_caleta')) || [];
}

function guardarCarrito(carrito) {
    localStorage.setItem('carrito_caleta', JSON.stringify(carrito));
    actualizarVistasCarrito();
}

function agregarAlCarrito(nombre, precio, imagenUrl) {
    const carrito = obtenerCarrito();
    const itemExistente = carrito.find(item => item.nombre === nombre);

    if (itemExistente) {
        itemExistente.cantidad += 1;
    } else {
        carrito.push({
            nombre: nombre,
            precio: parseFloat(precio),
            cantidad: 1,
            imagenUrl: imagenUrl
        });
    }

    guardarCarrito(carrito);
    
}

function cambiarCantidad(indice, cambio) {
    const carrito = obtenerCarrito();
    if (carrito[indice]) {
        carrito[indice].cantidad += cambio;
        if (carrito[indice].cantidad <= 0) {
            carrito.splice(indice, 1);
        }
        guardarCarrito(carrito);
    }
}

function eliminarDelCarrito(indice) {
    const carrito = obtenerCarrito();
    if (carrito[indice]) {
        carrito.splice(indice, 1);
        guardarCarrito(carrito);
    }
}

function vaciarCarrito() {
    localStorage.removeItem('carrito_caleta');
    actualizarVistasCarrito();
}

function actualizarVistasCarrito() {
    const carrito = obtenerCarrito();
    let subtotal = 0;
    let cantidadTotal = 0;

    carrito.forEach(item => {
        subtotal += item.precio * item.cantidad;
        cantidadTotal += item.cantidad;
    });

    const costoEnvio = carrito.length > 0 ? 5.00 : 0.00;
    const total = subtotal + costoEnvio;

    // --- A. ACTUALIZAR TEXTO/BADGE DEL BOTÓN DEL CARRITO EN NAVBAR ---
    const badgeMonto = document.querySelector('.nav-menu-link-shop span');
    if (badgeMonto) {
        badgeMonto.innerText = `S/ ${subtotal.toFixed(2)}`;
    }

    // --- B. RENDERIZAR OFFCANVAS / MODAL LATERAL ---
    const offcanvasBody = document.querySelector('#offcanvasCarrito .offcanvas-body');
    if (offcanvasBody) {
        if (carrito.length === 0) {
            offcanvasBody.innerHTML = `
                <div class="text-center my-auto">
                    <div class="mb-3">
                        <i class="fa-solid fa-basket-shopping fs-1 text-secondary opacity-50"></i>
                    </div>
                    <h6 class="fw-bold text-white mb-2">Tu carrito está vacío</h6>
                    <p class="text-secondary small mb-4">
                        Aún no has agregado productos. Explora nuestras pizzas secretas.
                    </p>
                    <a href="/menu" class="btn-cta px-4 py-2 fw-bold text-uppercase d-inline-block small text-decoration-none">
                        Explorar Menú
                    </a>
                </div>
                <div class="border-top border-secondary border-opacity-25 pt-3">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <span class="text-secondary small">Total estimado:</span>
                        <span class="fw-bold fs-5 text-white">S/ 0.00</span>
                    </div>
                    <button class="btn btn-secondary w-100 fw-bold py-2 text-uppercase small disabled" style="border-radius: 30px;">
                        Continuar con el pedido
                    </button>
                </div>
            `;
        } else {
            const itemsHtml = carrito.map((item, idx) => `
                <div class="d-flex align-items-center justify-content-between py-2 border-bottom border-secondary border-opacity-25">
                    <img src="${item.imagenUrl}" alt="${item.nombre}" class="rounded object-fit-cover" style="width: 48px; height: 48px;">
                    <div class="flex-grow-1 ms-3">
                        <h6 class="mb-0 text-white small fw-bold">${item.nombre}</h6>
                        <span class="text-secondary small">S/ ${item.precio.toFixed(2)} x ${item.cantidad}</span>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <button onclick="cambiarCantidad(${idx}, -1)" class="btn btn-sm btn-outline-secondary py-0 px-2 text-white">-</button>
                        <span class="small fw-bold text-white">${item.cantidad}</span>
                        <button onclick="cambiarCantidad(${idx}, 1)" class="btn btn-sm btn-outline-secondary py-0 px-2 text-white">+</button>
                        <button onclick="eliminarDelCarrito(${idx})" class="btn btn-sm text-danger p-0 ms-1 border-0">
                            <i class="fa-solid fa-trash-can"></i>
                        </button>
                    </div>
                </div>
            `).join('');

            offcanvasBody.innerHTML = `
                <div class="overflow-auto flex-grow-1 mb-3 pe-1">
                    ${itemsHtml}
                </div>
                <div class="border-top border-secondary border-opacity-25 pt-3">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <span class="text-secondary small">Subtotal:</span>
                        <span class="fw-bold fs-5 text-white">S/ ${subtotal.toFixed(2)}</span>
                    </div>
                    <a href="/carrito" class="btn btn-danger w-100 fw-bold py-2 text-uppercase small text-decoration-none text-center d-block" style="border-radius: 30px;">
                        Ver Carrito / Pagar
                    </a>
                </div>
            `;
        }
    }

    // --- C. RENDERIZAR TABLA EN CHECKOUT/CART.HTML ---
    const tablaItems = document.getElementById('carrito-items');
    const subtotalDOM = document.getElementById('resumen-subtotal');
    const totalDOM = document.getElementById('resumen-total');

    if (tablaItems) {
        if (carrito.length === 0) {
            tablaItems.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center py-5 text-secondary">
                        Tu carrito está vacío. <a href="/menu" class="text-danger text-decoration-none ms-1">Ver carta</a>
                    </td>
                </tr>
            `;
        } else {
            tablaItems.innerHTML = carrito.map((item, idx) => `
                <tr>
                    <td class="ps-4 py-3">
                        <div class="d-flex align-items-center gap-3">
                            <img src="${item.imagenUrl}" alt="${item.nombre}" class="rounded-2 object-fit-cover" style="width: 55px; height: 55px;">
                            <div>
                                <h6 class="fw-bold mb-0 text-white">${item.nombre}</h6>
                                <button onclick="eliminarDelCarrito(${idx})" class="btn btn-link text-danger p-0 text-decoration-none small border-0 mt-1" style="font-size: 11px;">
                                    <i class="fa-solid fa-trash-can me-1"></i> Eliminar
                                </button>
                            </div>
                        </div>
                    </td>
                    <td class="text-center py-3">
                        <div class="d-inline-flex align-items-center border border-secondary border-opacity-50 rounded-pill px-2 py-1">
                            <button onclick="cambiarCantidad(${idx}, -1)" class="btn btn-sm text-white p-0 border-0 shadow-none"><i class="fa-solid fa-minus fs-xs"></i></button>
                            <span class="px-2 fw-semibold small">${item.cantidad}</span>
                            <button onclick="cambiarCantidad(${idx}, 1)" class="btn btn-sm text-white p-0 border-0 shadow-none"><i class="fa-solid fa-plus fs-xs"></i></button>
                        </div>
                    </td>
                    <td class="text-end text-secondary small py-3">S/ ${item.precio.toFixed(2)}</td>
                    <td class="text-end fw-bold text-white py-3 pe-4">S/ ${(item.precio * item.cantidad).toFixed(2)}</td>
                </tr>
            `).join('');
        }
    }

    if (subtotalDOM) subtotalDOM.innerText = `S/ ${subtotal.toFixed(2)}`;
    if (totalDOM) totalDOM.innerText = `S/ ${total.toFixed(2)}`;
}

document.addEventListener("DOMContentLoaded", actualizarVistasCarrito);


function mostrarNotificacionCarrito(nombreProducto) {
    const toastEl = document.getElementById('toastCarrito');
    const toastMensaje = document.getElementById('toastMensaje');

    if (toastEl && toastMensaje) {
        toastMensaje.innerHTML = `<strong>${nombreProducto}</strong> se agregó al pedido.`;
        const toast = bootstrap.Toast.getOrCreateInstance(toastEl, { delay: 2500 });
        toast.show();
    }
}

function agregarAlCarrito(nombre, precio, imagenUrl) {
    const carrito = obtenerCarrito();
    const itemExistente = carrito.find(item => item.nombre === nombre);

    if (itemExistente) {
        itemExistente.cantidad += 1;
    } else {
        carrito.push({
            nombre: nombre,
            precio: parseFloat(precio),
            cantidad: 1,
            imagenUrl: imagenUrl
        });
    }

    guardarCarrito(carrito);

    mostrarNotificacionCarrito(nombre);
}