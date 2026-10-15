function obtenerCarrito() {
    return JSON.parse(localStorage.getItem('carrito_caleta')) || [];
}

function guardarCarrito(carrito) {
    localStorage.setItem('carrito_caleta', JSON.stringify(carrito));
    actualizarVistasCarrito();
}

function agregarAlCarrito(nombre, precio, imagenUrl, descripcion = "") {
    const carrito = obtenerCarrito();
    const itemExistente = carrito.find(item => item.nombre === nombre);
    if (itemExistente) {
        itemExistente.cantidad += 1;
    } else {
        carrito.push({
            nombre: nombre,
            precio: parseFloat(precio),
            cantidad: 1,
            imagenUrl: imagenUrl,
            descripcion: descripcion
        });
    }
    guardarCarrito(carrito);
    mostrarNotificacionCarrito(nombre);
}

function editarProductoCarrito(indice) {
    const carrito = obtenerCarrito();
    const item = carrito[indice];
    if (!item) return;

    const info = parsearNombreProducto(item.nombre);
    
    const params = new URLSearchParams({
        nombre: info.nombre,
        precio: item.precio,
        img: item.imagenUrl,
        desc: item.descripcion || '',
        editar: indice
    });

    window.location.href = `/detalles?${params.toString()}`;
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

function alternarInfo(idElemento, boton) {
    const contenedor = document.getElementById(idElemento);
    if (!contenedor) return;
    const estaOculto = contenedor.classList.contains("d-none");
    contenedor.classList.toggle("d-none");
    boton.innerHTML = estaOculto
        ? 'Ocultar información <i class="fa-solid fa-chevron-up ms-1 fs-xs"></i>'
        : 'Ver información <i class="fa-solid fa-chevron-down ms-1 fs-xs"></i>';
}

function parsearNombreProducto(nombreCompleto) {
    const pos = nombreCompleto.indexOf('(');
    if (pos !== -1) {
        return {
            nombre: nombreCompleto.substring(0, pos).trim(),
            detalles: nombreCompleto.substring(pos).trim()
        };
    }
    return {
        nombre: nombreCompleto,
        detalles: ''
    };
}

function formatearDetallesComoLista(detallesTexto) {
    if (!detallesTexto) return '';

    const partes = detallesTexto.match(/[(\[][^)\]]+[)\]]/g);

    if (partes && partes.length > 0) {
        return partes.map(parte => {
            const limpio = parte.replace(/[()\[\]]/g, '').trim();
            return `<div class="py-0.5">• ${limpio}</div>`;
        }).join('');
    }

    return detallesTexto
        .replace(/[()\[\]]/g, '')
        .split(',')
        .map(item => item.trim())
        .filter(item => item.length > 0)
        .map(item => `<div class="py-0.5">• ${item}</div>`)
        .join('');
}

function actualizarVistasCarrito() {
    const carrito = obtenerCarrito();
    let subtotal = 0;
    carrito.forEach(item => {
        subtotal += item.precio * item.cantidad;
    });

    const costoEnvio = carrito.length > 0 ? 5.00 : 0.00;
    const total = subtotal + costoEnvio;

    const badgeMonto = document.querySelector('.nav-menu-link-shop span');
    if (badgeMonto) {
        badgeMonto.innerText = `S/ ${subtotal.toFixed(2)}`;
    }

    const offcanvasBody = document.querySelector('#offcanvasCarrito .offcanvas-body');
    if (offcanvasBody) {
        if (carrito.length === 0) {
            offcanvasBody.innerHTML = `
                <div class="text-center my-auto">
                    <div class="mb-3">
                        <i class="fa-solid fa-basket-shopping fs-1 text-secondary opacity-50"></i>
                    </div>
                    <h6 class="fw-bold text-white mb-2">Tu carrito está vacío</h6>
                    <p class="text-secondary small mb-4">Aún no has agregado productos. Explora nuestras pizzas secretas.</p>
                    <a href="/menu" class="btn-cta px-4 py-2 fw-bold text-uppercase d-inline-block small text-decoration-none">Explorar Menú</a>
                </div>
                <div class="border-top border-secondary border-opacity-25 pt-3">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <span class="text-secondary small">Total estimado:</span>
                        <span class="fw-bold fs-5 text-white">S/ 0.00</span>
                    </div>
                    <button class="btn btn-secondary w-100 fw-bold py-2 text-uppercase small disabled" style="border-radius: 30px;">Continuar con el pedido</button>
                </div>
            `;
        } else {
            const itemsHtml = carrito.map((item, idx) => {
                const info = parsearNombreProducto(item.nombre);

                return `
                    <div class="d-flex align-items-start justify-content-between py-3 border-bottom border-secondary border-opacity-25 gap-2">
                        <img src="${item.imagenUrl}" alt="${info.nombre}" class="rounded object-fit-cover flex-shrink-0" style="width: 48px; height: 48px;">
                        <div class="flex-grow-1 ms-2 min-w-0">
                            <h6 class="mb-1 text-white small fw-bold text-truncate">${info.nombre}</h6>
                            
                            ${info.detalles ? `
                                <div class="mb-1 d-flex align-items-center gap-2">
                                    <button type="button" class="btn-ver-info" onclick="alternarInfo('offcanvas-info-${idx}', this)">
                                        Ver información <i class="fa-solid fa-chevron-down ms-1 fs-xs"></i>
                                    </button>
                                    <button type="button" class="btn btn-link text-secondary p-0 text-decoration-none fs-xs hover-rojo" onclick="editarProductoCarrito(${idx})">
                                        <i class="fa-solid fa-pen-to-square me-1"></i> Editar
                                    </button>
                                </div>
                                <div id="offcanvas-info-${idx}" class="d-none text-white-50 fs-xs mt-1 ps-2 border-start border-secondary">
                                    ${formatearDetallesComoLista(info.detalles)}
                                </div>
                            ` : ''}

                            <span class="text-secondary fs-xs">S/ ${item.precio.toFixed(2)} x ${item.cantidad}</span>
                        </div>
                        <div class="d-flex align-items-center gap-2 flex-shrink-0">
                            <button onclick="cambiarCantidad(${idx}, -1)" class="btn btn-sm btn-outline-secondary py-0 px-2 text-white">-</button>
                            <span class="small fw-bold text-white">${item.cantidad}</span>
                            <button onclick="cambiarCantidad(${idx}, 1)" class="btn btn-sm btn-outline-secondary py-0 px-2 text-white">+</button>
                            <button onclick="eliminarDelCarrito(${idx})" class="btn btn-sm text-danger p-0 ms-1 border-0">
                                <i class="fa-solid fa-trash-can"></i>
                            </button>
                        </div>
                    </div>
                `;
            }).join('');

            offcanvasBody.innerHTML = `
                <div class="overflow-auto flex-grow-1 mb-3 pe-1">
                    ${itemsHtml}
                </div>
                <div class="border-top border-secondary border-opacity-25 pt-3">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <span class="text-secondary small">Subtotal:</span>
                        <span class="fw-bold fs-5 text-white">S/ ${subtotal.toFixed(2)}</span>
                    </div>
                    <a href="/carrito" class="btn-cta w-100 fw-bold py-2 text-uppercase small text-decoration-none text-center d-block">
                        Ver Carrito / Pagar
                    </a>
                </div>
            `;
        }
    }

    const tablaItems = document.getElementById('carrito-items');
    const subtotalDOM = document.getElementById('resumen-subtotal');
    const totalDOM = document.getElementById('resumen-total');
    if (tablaItems) {
        if (carrito.length === 0) {
            tablaItems.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center py-5 text-secondary">
                        Tu carrito está vacío. <a href="/menu" class="text-ro text-decoration-none ms-1">Ver carta</a>
                    </td>
                </tr>
            `;
        } else {
            tablaItems.innerHTML = carrito.map((item, idx) => {
                const info = parsearNombreProducto(item.nombre);

                return `
                    <tr>
                        <td class="ps-4 py-3">
                            <div class="d-flex align-items-center gap-3">
                                <img src="${item.imagenUrl}" alt="${info.nombre}" class="rounded-2 object-fit-cover" style="width: 55px; height: 55px;">
                                <div>
                                    <h6 class="fw-bold mb-1 text-white">${info.nombre}</h6>
                                    
                                    ${info.detalles ? `
                                        <div class="mb-1 d-flex align-items-center gap-2">
                                            <button type="button" class="btn-ver-info" onclick="alternarInfo('table-info-${idx}', this)">
                                                Ver información <i class="fa-solid fa-chevron-down ms-1 fs-xs"></i>
                                            </button>
                                            <button type="button" class="btn btn-link text-secondary p-0 text-decoration-none fs-xs hover-rojo" onclick="editarProductoCarrito(${idx})">
                                                <i class="fa-solid fa-pen-to-square me-1"></i> Editar
                                                </button>
                                        </div>
                                        <div id="table-info-${idx}" class="d-none text-white-50 fs-xs mt-1 ps-2 border-start border-secondary">
                                            ${formatearDetallesComoLista(info.detalles)}
                                        </div>
                                    ` : ''}

                                    <button onclick="eliminarDelCarrito(${idx})" class="btn btn-link text-danger p-0 text-decoration-none border-0 mt-1" style="font-size: 11px;">
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
                `;
            }).join('');
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
        const info = parsearNombreProducto(nombreProducto);
        toastMensaje.innerHTML = `<strong>${info.nombre}</strong> se agregó al pedido.`;
        const toast = bootstrap.Toast.getOrCreateInstance(toastEl, { delay: 2500 });
        toast.show();
    }
}

function editarProductoCarrito(indice) {
    const carrito = obtenerCarrito();
    const item = carrito[indice];
    if (!item) return;

    const info = parsearNombreProducto(item.nombre);

    const params = new URLSearchParams({
        nombre: info.nombre,
        precio: item.precio,
        img: item.imagenUrl,
        desc: item.descripcion || '',
        editar: indice
    });

    window.location.href = `/detalles?${params.toString()}`;
}