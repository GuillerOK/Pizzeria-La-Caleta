const API_URL = 'https://pizzeria-la-caleta.onrender.com/api';

// Verificar acceso para la vista de administración
function verificarAccesoAdmin() {
    if (window.location.pathname.includes("/administracion")) {
        const sesion = JSON.parse(localStorage.getItem("sesion_activa"));
        if (!sesion || sesion.rol !== "ADMIN") {
            alert("Acceso no autorizado. Debe iniciar sesión como administrador.");
            window.location.href = "/login";
        }
    }
}

// Sincronizar estado de sesión en la barra de navegación
function actualizarNavbar() {
    const sesion = JSON.parse(localStorage.getItem("sesion_activa"));
    const navUserArea = document.getElementById("navUserArea");
    const navLinkMenu = document.getElementById("navLinkMenu");

    if (!navUserArea) return;

    if (sesion) {
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

        if (sesion.rol === "ADMIN" && !window.location.pathname.includes("/administracion")) {
            if (navLinkMenu && !document.getElementById("navAdminLink")) {
                const adminBtn = document.createElement("a");
                adminBtn.id = "navAdminLink";
                adminBtn.className = "nav-link px-3 py-1 fw-bold text-warning text-nowrap";
                adminBtn.href = "/administracion";
                adminBtn.innerHTML = '<i class="fa-solid fa-lock"></i> Administración';
                navLinkMenu.after(adminBtn);
            }
        }
    } else {
        navUserArea.innerHTML = `
            <a class="d-flex align-items-center gap-2 text-white text-nowrap" href="/login">
                <i class="fa-solid fa-user"></i>
                <span>Iniciar sesión</span>
            </a>
        `;
    }
}

function cerrarSesion() {
    localStorage.removeItem("sesion_activa");
    localStorage.removeItem("token");
    window.location.href = "/login";
}

// Inicialización de formularios de cuenta
document.addEventListener("DOMContentLoaded", () => {
    // Formulario de Registro
    const formRegistro = document.getElementById("formRegistro");
    if (formRegistro) {
        formRegistro.addEventListener("submit", async (e) => {
            e.preventDefault();
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
                const respuesta = await fetch(`${API_URL}/register`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ nombre, apellido, email, telefono, password })
                });
                const datos = await respuesta.json();

                if (respuesta.ok) {
                    alert("¡Registro exitoso! Ya puedes iniciar sesión.");
                    window.location.href = "/login";
                } else {
                    alert(`Error: ${datos.error}`);
                }
            } catch (error) {
                console.error(error);
                alert("Error crítico: No se pudo conectar con el servidor.");
            }
        });
    }

    // Formulario de Login
    const formLogin = document.getElementById("formLogin");
    if (formLogin) {
        formLogin.addEventListener("submit", async (e) => {
            e.preventDefault();
            const email = document.getElementById("loginEmail").value.trim().toLowerCase();
            const password = document.getElementById("loginPassword").value;

            try {
                const respuesta = await fetch(`${API_URL}/login`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password })
                });
                const datos = await respuesta.json();

                if (respuesta.ok) {
                    localStorage.setItem('token', datos.token);
                    localStorage.setItem("sesion_activa", JSON.stringify(datos.usuario));

                    if (datos.usuario.rol === "ADMIN") {
                        alert(`Bienvenido Administrador: ${datos.usuario.nombre}`);
                        window.location.href = "/administracion";
                    } else {
                        alert(`Bienvenido ${datos.usuario.nombre}`);
                        window.location.href = "/";
                    }
                } else {
                    alert(`Error: ${datos.error}`);
                }
            } catch (error) {
                console.error(error);
                alert("Error crítico: No se pudo conectar con el servidor.");
            }
        });
    }

    // Toggle ver contraseña
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