function desplazar(id, desplazamiento) {
    const contenedor = document.getElementById(id);
    if (contenedor) {
        contenedor.scrollBy({ left: desplazamiento, behavior: 'smooth' });
    }
}

function cambiarSeccion(idSeccion, botonPresionado = null) {
    const secciones = document.querySelectorAll('.seccion-menu');
    secciones.forEach(sec => sec.classList.add('d-none'));

    const seccionActiva = document.getElementById(idSeccion);
    if (seccionActiva) {
        seccionActiva.classList.remove('d-none');
    }

    const botones = document.querySelectorAll('.btn-categoria');
    botones.forEach(btn => btn.classList.remove('active'));

    if (botonPresionado) {
        botonPresionado.classList.add('active');
    } else {
        const botonCoincidente = Array.from(botones).find(btn => 
            btn.getAttribute('onclick')?.includes(idSeccion)
        );
        if (botonCoincidente) {
            botonCoincidente.classList.add('active');
        }
    }
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

window.addEventListener('DOMContentLoaded', procesarHash);
window.addEventListener('hashchange', procesarHash);

function procesarHash() {
    const hash = window.location.hash.replace('#', '');
    if (hash && document.getElementById(hash)) {
        cambiarSeccion(hash);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    // 1. Control de ruta protegida para admin.html
    verificarAccesoAdmin();

    // 2. Sincronizar estado de sesión en la barra de navegación de cualquier página
    actualizarNavbar();

    // --- REGISTRO DE CUENTAS (create.html) ---
    // (Aún usa LocalStorage, lo conectaremos a Node.js en el siguiente paso)
    const formRegistro = document.getElementById("formRegistro");
    if (formRegistro) {
        formRegistro.addEventListener("submit", async (e) => {
            e.preventDefault();
            
            // Capturamos los datos (Asegúrate de que estos IDs coincidan con tu HTML)
            const nombre = document.getElementById("regNombre")?.value.trim() || "";
            const apellido = document.getElementById("regApellido")?.value.trim() || "";
            const email = document.getElementById("regEmail")?.value.trim().toLowerCase() || "";
            const telefono = document.getElementById("regTelefono")?.value.trim() || null;
            const password = document.getElementById("regPassword")?.value || "";
            const confirmPassword = document.getElementById("regPasswordConfirm")?.value || "";

            if (password !== confirmPassword) {
                alert("Las contraseñas no coinciden. Por favor verifica.");
                return;
            }

            try {
                const respuesta = await fetch('https://pizzeria-la-caleta.onrender.com/api/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ nombre, apellido, email, telefono, password })
                });

                const datos = await respuesta.json();

                if (respuesta.ok) {
                    alert("¡Registro exitoso! Ya puedes iniciar sesión.");
                    window.location.href = "login.html";
                } else {
                    alert(`Error: ${datos.error}`);
                }
            } catch (error) {
                console.error(error);
                alert("Error crítico: No se pudo conectar con el servidor.");
            }
        });
    }

    // --- INICIO DE SESIÓN (login.html) --- CONECTADO A NODE.JS
    const formLogin = document.getElementById("formLogin");
    if (formLogin) {
        formLogin.addEventListener("submit", async (e) => { 
            e.preventDefault();

            const email = document.getElementById("loginEmail").value.trim().toLowerCase();
            const password = document.getElementById("loginPassword").value;

            try {
                // Petición real al servidor Node.js
                const respuesta = await fetch('https://pizzeria-la-caleta.onrender.com/api/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password })
                });

                const datos = await respuesta.json();

                if (respuesta.ok) {
                    // Guardamos el token de seguridad y los datos de sesión
                    localStorage.setItem('token', datos.token);
                    localStorage.setItem("sesion_activa", JSON.stringify(datos.usuario));

                    // Redirección dinámica basada en roles
                    if (datos.usuario.rol === "ADMIN") {
                        alert(`Bienvenido Administrador: ${datos.usuario.nombre}`);
                        // Sube una carpeta (sale de account) y entra a admin
                        window.location.href = "../admin/admin.html";
                    } else {
                        alert(`Bienvenido ${datos.usuario.nombre}`);
                        // Sube dos carpetas (sale de account, sale de pages) y llega a la raíz
                        window.location.href = "../../index.html";
                    }
                } else {
                    alert(`Error: ${datos.error}`);
                }
            } catch (error) {
                console.error(error);
                alert("Error crítico: No se pudo conectar con el servidor Node.js. ¿Está encendido?");
            }
        });
    }

    // --- ALTERNAR VISIBILIDAD DE CONTRASEÑA ---
    const toggleEye = document.getElementById("toggleEye");
    if (toggleEye) {
        toggleEye.addEventListener("click", () => {
            const passInput = document.getElementById("loginPassword");
            if (passInput.type === "password") {
                passInput.type = "text";
                toggleEye.classList.replace("fa-eye", "fa-eye-slash");
            } else {
                passInput.type = "password";
                toggleEye.classList.replace("fa-eye-slash", "fa-eye");
            }
        });
    }
});

// Proteger ruta de administración
function verificarAccesoAdmin() {
    if (window.location.pathname.includes("admin.html")) {
        const sesion = JSON.parse(localStorage.getItem("sesion_activa"));
        if (!sesion || sesion.rol !== "ADMIN") {
            alert("Acceso no autorizado. Debe iniciar sesión como administrador.");
            window.location.href = "login.html";
        }
    }
}

// Actualizar barra de navegación en todas las páginas según la sesión activa
function actualizarNavbar() {
    const sesion = JSON.parse(localStorage.getItem("sesion_activa"));
    const navUserArea = document.getElementById("navUserArea");
    const navLinkMenu = document.getElementById("navLinkMenu");

    if (!navUserArea) return;

    if (sesion) {
        // 1. Mostrar nombre y botón para cerrar sesión
        const iconoRol = sesion.rol === "ADMIN" ? "fa-user-shield text-warning" : "fa-user-check text-success";
        navUserArea.innerHTML = `
            <div class="d-flex align-items-center gap-2 text-white text-nowrap">
                <i class="fa-solid ${iconoRol}"></i>
                <span>${sesion.nombre}</span>
                <a href="#" onclick="cerrarSesion()" class="text-danger small ms-2 text-decoration-none" title="Cerrar sesión">
                    <i class="fa-solid fa-right-from-bracket"></i> Salir
                </a>
            </div>
        `;

        // 2. Si es admin y no está en admin.html, insertar la pestaña 'Administración'
        if (sesion.rol === "ADMIN" && !window.location.pathname.includes("admin.html")) {
            if (navLinkMenu && !document.getElementById("navAdminLink")) {
                const adminBtn = document.createElement("a");
                adminBtn.id = "navAdminLink";
                adminBtn.className = "nav-link px-3 py-1 fw-bold text-warning text-nowrap";
                adminBtn.href = "admin.html";
                adminBtn.innerHTML = '<i class="fa-solid fa-lock"></i> Administración';
                navLinkMenu.after(adminBtn);
            }
        }
    } else {
        // Si no hay sesión activa, mantener botón de inicio de sesión
        navUserArea.innerHTML = `
            <a class="d-flex align-items-center gap-2 text-white text-nowrap" href="login.html">
                <i class="fa-solid fa-user"></i>
                <span>Iniciar sesión</span>
            </a>
        `;
    }
}

// Cerrar sesión y redireccionar

function cerrarSesion() {
    localStorage.removeItem("sesion_activa");
    localStorage.removeItem("token");
    
    if (window.location.pathname.includes('/pages/')) {
        window.location.href = "../account/login.html";
    } else {
        window.location.href = "pages/account/login.html";
    }
}



 // CARRITO DE COMPRAS DINÁMICO

// 1. Inicializar o leer el carrito desde localStorage
let carrito = JSON.parse(localStorage.getItem('carrito_caleta')) || [];

// 2. Función para agregar productos (se llamará desde los botones del menú)

function agregarAlCarrito(nombre, precio, imagenUrl) {
    // Buscamos si la pizza ya está en el carrito
    const itemExistente = carrito.find(item => item.nombre === nombre);

    if (itemExistente) {
        itemExistente.cantidad += 1; // Si existe, sumamos 1 a la cantidad
    } else {
        // Si no existe, lo agregamos como un nuevo objeto
        carrito.push({
            nombre: nombre,
            precio: parseFloat(precio),
            cantidad: 1,
            imagenUrl: imagenUrl
        });
    }

    // Guardamos en el navegador y mostramos una pequeña alerta
    localStorage.setItem('carrito_caleta', JSON.stringify(carrito));
    alert(`¡${nombre} se agregó al carrito!`);
    
    // (Opcional) Si quieres actualizar el número en el ícono del carrito arriba, lo haríamos aquí
}

// 3. Función para renderizar (dibujar) el carrito en cart.html
function renderizarCarrito() {
    // Verificamos si estamos en la página del carrito
    const contenedorItems = document.getElementById('carrito-items');
    if (!contenedorItems) return; // Si no estamos en cart.html, detenemos la función

    const subtotalDOM = document.getElementById('resumen-subtotal');
    const totalDOM = document.getElementById('resumen-total');
    
    // Si el carrito está vacío, limpiamos la tabla
    if (carrito.length === 0) {
        contenedorItems.innerHTML = `<tr><td colspan="4" class="text-center py-4 text-secondary">Tu carrito está vacío. ¡Ve al menú y elige tu pizza secreta!</td></tr>`;
        subtotalDOM.innerText = "S/ 0.00";
        totalDOM.innerText = "S/ 0.00";
        return;
    }

    // Limpiamos la tabla estática y empezamos a sumar
    contenedorItems.innerHTML = '';
    let subtotalPagar = 0;

    // Recorremos cada producto del carrito para crear su fila HTML
    carrito.forEach((item, index) => {
        const subtotalItem = item.precio * item.cantidad;
        subtotalPagar += subtotalItem;

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td class="ps-4 py-3">
                <div class="d-flex align-items-center gap-3">
                    <img src="${item.imagenUrl}" alt="${item.nombre}" class="rounded-2 object-fit-cover" style="width: 55px; height: 55px;">
                    <div>
                        <h6 class="fw-bold mb-0 text-white">${item.nombre}</h6>
                        <button onclick="eliminarDelCarrito(${index})" class="btn btn-link text-danger p-0 text-decoration-none small border-0 mt-1" style="font-size: 11px;">
                            <i class="fa-solid fa-trash-can me-1"></i> Eliminar
                        </button>
                    </div>
                </div>
            </td>
            <td class="text-center py-3">
                <div class="d-inline-flex align-items-center border border-secondary border-opacity-50 rounded-pill px-2 py-1">
                    <button onclick="cambiarCantidad(${index}, -1)" class="btn btn-sm text-white p-0 border-0 shadow-none"><i class="fa-solid fa-minus fs-xs"></i></button>
                    <span class="px-2 fw-semibold small">${item.cantidad}</span>
                    <button onclick="cambiarCantidad(${index}, 1)" class="btn btn-sm text-white p-0 border-0 shadow-none"><i class="fa-solid fa-plus fs-xs"></i></button>
                </div>
            </td>
            <td class="text-end text-secondary small py-3">S/ ${item.precio.toFixed(2)}</td>
            <td class="text-end fw-bold text-white py-3 pe-4">S/ ${subtotalItem.toFixed(2)}</td>
        `;
        contenedorItems.appendChild(tr);
    });

    // Actualizamos los totales (El costo de envío es 5.00 fijo por ahora según tu HTML)
    subtotalDOM.innerText = `S/ ${subtotalPagar.toFixed(2)}`;
    const costoEnvio = 5.00;
    const totalPagar = subtotalPagar + costoEnvio;
    totalDOM.innerText = `S/ ${totalPagar.toFixed(2)}`;
}

// 4. Funciones auxiliares para modificar cantidades y eliminar
function cambiarCantidad(indice, variacion) {
    carrito[indice].cantidad += variacion;
    if (carrito[indice].cantidad <= 0) {
        eliminarDelCarrito(indice);
    } else {
        localStorage.setItem('carrito_caleta', JSON.stringify(carrito));
        renderizarCarrito(); // Redibujamos la tabla
    }
}

function eliminarDelCarrito(indice) {
    carrito.splice(indice, 1); // Eliminamos el elemento del arreglo
    localStorage.setItem('carrito_caleta', JSON.stringify(carrito));
    renderizarCarrito(); // Redibujamos la tabla
}

// Ejecutar la renderización cuando cargue la página
document.addEventListener("DOMContentLoaded", () => {
    renderizarCarrito();
}); 


// ==========================================
// LÓGICA DEL CARRITO (Conectado a Render)
// ==========================================
async function agregarAlCarrito(productoVarianteId) {
    // 1. Buscamos la "pulsera VIP" del usuario en el navegador
    const token = localStorage.getItem('token');
    
    // 2. Si no tiene token (no está logueado), le bloqueamos el paso y lo mandamos a login
    if (!token) {
        alert("Debes iniciar sesión para añadir productos al carrito.");

        // Ruta inteligente dependiendo de dónde esté el usuario
        if (window.location.pathname.includes('/pages/')) {
            window.location.href = "../account/login.html"; // Si está en el menú u otra subpágina
        } else {
            window.location.href = "pages/account/login.html"; // Si está en la pantalla principal (index)
        }
        return;
    }

    // 3. Si está logueado, hacemos la petición a tu servidor en Render
    try {
        const response = await fetch('https://pizzeria-la-caleta.onrender.com/api/carrito/add', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token // Tu Middleware exige esto
            },
            body: JSON.stringify({
                productoVarianteId: productoVarianteId,
                cantidad: 1
            })
        });

        const data = await response.json();

        if (response.ok) {
            alert(data.mensaje); // Mostrará "Producto añadido al carrito."
        } else {
            alert(data.error || "Ocurrió un error al intentar agregar el producto.");
        }
    } catch (error) {
        console.error("Error:", error);
        alert("Error de conexión con el servidor. Verifica tu internet o si el servidor está activo.");
    }
}