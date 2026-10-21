// Escapar datos de catálogo, URL y almacenamiento antes de interpolarlos en HTML.
function escaparHtml(valor) {
    return String(valor ?? '').replace(/[&<>"']/g, caracter => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    })[caracter]);
}

function obtenerCarrito() {
    return JSON.parse(localStorage.getItem('carrito_caleta')) || [];
}

function guardarCarrito(carrito) {
    localStorage.setItem('carrito_caleta', JSON.stringify(carrito));
    actualizarVistasCarrito();
}

function agregarAlCarrito(nombre, precio, imagenUrl, descripcion = "", productoId = null) {
    const carrito = obtenerCarrito();
    const itemExistente = carrito.find(item => item.nombre === nombre && item.productoId === productoId);
    if (itemExistente) {
        itemExistente.cantidad += 1;
    } else {
        carrito.push({
            productoId: productoId,
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
            return `<div class="py-0.5">• ${escaparHtml(limpio)}</div>`;
        }).join('');
    }

    return detallesTexto
        .replace(/[()\[\]]/g, '')
        .split(',')
        .map(item => item.trim())
        .filter(item => item.length > 0)
        .map(item => `<div class="py-0.5">• ${escaparHtml(item)}</div>`)
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
                <div class="carrito-resumen border-top border-secondary border-opacity-25 pt-3">
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
                        <img src="${escaparHtml(item.imagenUrl)}" alt="${escaparHtml(info.nombre)}" class="rounded object-fit-cover flex-shrink-0" style="width: 48px; height: 48px;">
                        <div class="flex-grow-1 ms-2 min-w-0">
                            <h6 class="mb-1 text-white small fw-bold text-truncate">${escaparHtml(info.nombre)}</h6>
                            
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
                <div class="carrito-productos flex-grow-1 mb-3 pe-1">
                    ${itemsHtml}
                </div>
                <div class="carrito-resumen border-top border-secondary border-opacity-25 pt-3">
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
                                <img src="${escaparHtml(item.imagenUrl)}" alt="${escaparHtml(info.nombre)}" class="rounded-2 object-fit-cover" style="width: 55px; height: 55px;">
                                <div>
                                    <h6 class="fw-bold mb-1 text-white">${escaparHtml(info.nombre)}</h6>
                                    
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
    // Manejo de cupón aplicado en el carrito
    const cuponGuardado = JSON.parse(localStorage.getItem('cupon_caleta') || 'null');
    let descuento = 0;
    const filaDescuento = document.getElementById('resumen-fila-descuento');
    const labelDescuento = document.getElementById('resumen-descuento-label');
    const valorDescuentoDOM = document.getElementById('resumen-descuento');
    const cardCuponAplicado = document.getElementById('cuponAplicadoCard');
    const tituloCuponAplicado = document.getElementById('cuponAplicadoTitulo');
    const detalleCuponAplicado = document.getElementById('cuponAplicadoDetalle');
    const grupoInputCupon = document.getElementById('grupoInputCupon');

    if (cuponGuardado && carrito.length > 0) {
        if (cuponGuardado.tipo === 'REGALO') {
            descuento = 0;
        } else if (cuponGuardado.tipo === 'DELIVERY_GRATIS') {
            descuento = costoEnvio;
        } else if (cuponGuardado.tipo === 'PORCENTAJE') {
            descuento = Math.round((subtotal * ((cuponGuardado.valorDescuento || cuponGuardado.descuento) / 100.0)) * 100) / 100;
        } else {
            descuento = Math.min(subtotal, cuponGuardado.valorDescuento ?? cuponGuardado.descuento ?? 0);
        }

        if (filaDescuento) {
            filaDescuento.classList.remove('d-none');
            if (valorDescuentoDOM) valorDescuentoDOM.innerText = cuponGuardado.tipo === 'REGALO' ? `${cuponGuardado.titulo} (CUPÓN REGALO) — S/ 0.00` : `- S/ ${descuento.toFixed(2)}`;
            if (labelDescuento) labelDescuento.innerHTML = `<i class="fa-solid fa-tag me-1"></i>${escaparHtml(cuponGuardado.titulo || 'Cupón')}`;
        }
        if (cardCuponAplicado) {
            cardCuponAplicado.classList.remove('d-none');
            if (tituloCuponAplicado) tituloCuponAplicado.innerText = cuponGuardado.titulo || 'Cupón aplicado';
            if (detalleCuponAplicado) detalleCuponAplicado.innerText = cuponGuardado.tipo === 'REGALO'
                ? `Código: ${cuponGuardado.codigo} — Producto de regalo a S/ 0.00`
                : `Código: ${cuponGuardado.codigo} (-S/ ${descuento.toFixed(2)})`;
        }
        if (grupoInputCupon) {
            grupoInputCupon.classList.add('d-none');
        }
    } else {
        if (filaDescuento) filaDescuento.classList.add('d-none');
        if (cardCuponAplicado) cardCuponAplicado.classList.add('d-none');
        if (grupoInputCupon) grupoInputCupon.classList.remove('d-none');
    }

    const totalFinal = Math.max(0, subtotal + costoEnvio - descuento);
    if (totalDOM) totalDOM.innerText = `S/ ${totalFinal.toFixed(2)}`;
}

function usarCuponEnCarrito(codigo) {
    localStorage.setItem('cupon_precargado_caleta', codigo);
    document.getElementById('inputCupon').value = codigo;
    const modal = document.getElementById('modalMisCupones');
    if (modal && window.bootstrap) bootstrap.Modal.getInstance(modal)?.hide();
    aplicarCupon();
}

async function copiarCodigoCupon(boton) {
    try {
        await navigator.clipboard.writeText(boton.dataset.codigo);
        boton.textContent = 'Copiado';
    } catch (_) {
        mostrarModalMensaje('Copiar cupón', 'Selecciona el código para copiarlo manualmente.');
    }
}

async function aplicarCupon() {
    const input = document.getElementById('inputCupon');
    const mensajeError = document.getElementById('cuponMensajeError');
    if (!input) return;
    const codigo = input.value.trim();

    if (!codigo) {
        if (mensajeError) {
            mensajeError.innerText = 'Por favor ingresa un código de cupón.';
            mensajeError.classList.remove('d-none');
        }
        return;
    }

    const carrito = obtenerCarrito();
    if (!carrito.length) {
        if (mensajeError) {
            mensajeError.innerText = 'Agrega productos a tu carrito antes de aplicar un cupón.';
            mensajeError.classList.remove('d-none');
        }
        return;
    }

    let subtotal = 0;
    carrito.forEach(i => subtotal += i.precio * i.cantidad);

    const btnAplicar = document.getElementById('btnAplicarCupon');
    if (btnAplicar) {
        btnAplicar.disabled = true;
        btnAplicar.innerText = 'Validando...';
    }
    if (mensajeError) mensajeError.classList.add('d-none');

    try {
        const respuesta = await fetch('/api/cupones/validar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ codigo, subtotal })
        });

        const data = await respuesta.json().catch(() => ({}));

        if (!respuesta.ok || !data.valido) {
            throw new Error(data.error || 'Cupón inválido o no disponible.');
        }

        localStorage.setItem('cupon_caleta', JSON.stringify({
            codigo: data.codigo,
            titulo: data.titulo,
            descripcion: data.descripcion,
            tipo: data.tipo,
            descuento: data.descuento,
            valorDescuento: data.valorDescuento
        }));

        localStorage.removeItem('cupon_precargado_caleta');
        input.value = '';
        actualizarVistasCarrito();
        mostrarModalMensaje('¡Cupón aplicado!', data.mensaje || 'Se ha aplicado el descuento a tu pedido.');
    } catch (err) {
        if (mensajeError) {
            mensajeError.innerText = err.message;
            mensajeError.classList.remove('d-none');
        } else {
            mostrarModalMensaje('Cupón no válido', err.message);
        }
    } finally {
        if (btnAplicar) {
            btnAplicar.disabled = false;
            btnAplicar.innerText = 'Aplicar';
        }
    }
}

function removerCupon() {
    localStorage.removeItem('cupon_precargado_caleta');
    localStorage.removeItem('cupon_caleta');
    const mensajeError = document.getElementById('cuponMensajeError');
    if (mensajeError) mensajeError.classList.add('d-none');
    actualizarVistasCarrito();
}

document.addEventListener("DOMContentLoaded", () => {
    actualizarVistasCarrito();

    const btnAplicar = document.getElementById('btnAplicarCupon');
    if (btnAplicar) {
        btnAplicar.addEventListener('click', aplicarCupon);
    }

    const inputCupon = document.getElementById('inputCupon');
    if (inputCupon) {
        inputCupon.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                aplicarCupon();
            }
        });
    }

    const btnRemover = document.getElementById('btnRemoverCupon');
    if (btnRemover) {
        btnRemover.addEventListener('click', removerCupon);
    }

    const precargado = localStorage.getItem('cupon_precargado_caleta');
    if (precargado) {
        if (inputCupon) {
            inputCupon.value = precargado;
            aplicarCupon();
        }
    }
});

function mostrarNotificacionCarrito(nombreProducto) {
    const info = parsearNombreProducto(nombreProducto);
    mostrarModalMensaje("Producto agregado", info.nombre + " se agregó al pedido.");
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


// El carrito continúa al formulario de envío; la compra se confirma en /pago.
document.addEventListener('DOMContentLoaded', () => {
    const boton = document.getElementById('btnComprar');
    if (!boton) return;
    boton.addEventListener('click', () => {
        if (!boton.dataset.email) {
            mostrarModalMensaje('Inicia sesión', 'Inicia sesión para comprar.', () => { window.location.href = '/login'; });
            return;
        }
        if (!obtenerCarrito().length) {
            mostrarModalMensaje('Carrito vacío', 'Agrega productos para continuar.');
            return;
        }
        window.location.href = '/envio';
    });
});
