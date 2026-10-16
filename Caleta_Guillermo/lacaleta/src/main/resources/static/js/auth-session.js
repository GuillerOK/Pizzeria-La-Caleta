/**
 * auth-session.js - Script para sincronizar el estado de sesión y actualizar el Navbar.
 */

document.addEventListener("DOMContentLoaded", function () {

    // 1. Obtener usuario almacenado si existe
    const sesion = JSON.parse(localStorage.getItem("sesion_activa") || localStorage.getItem("usuarioLogueado"));

    // 2. Si no hay área de usuario manipulable, finalizar
    const navUserArea = document.getElementById("navUserArea");
    if (!navUserArea) return;

    // 3. Si existe sesión local y no ha sido renderizada por Thymeleaf, reemplazar enlace de inicio de sesión
    if (sesion && sesion.nombre && !navUserArea.querySelector('.dropdown')) {
        navUserArea.innerHTML = `
            <div class="dropdown">
                <button class="btn btn-dark text-white dropdown-toggle border-0 small fw-semibold d-flex align-items-center gap-2 shadow-none" 
                        type="button" data-bs-toggle="dropdown" aria-expanded="false">
                    <i class="fa-solid fa-circle-user text-warning fs-5"></i>
                    <span>${sesion.nombre}</span>
                </button>
                <ul class="dropdown-menu dropdown-menu-end bg-dark border-secondary shadow">
                    <li>
                        <a class="dropdown-item text-white small" href="/logout" onclick="localStorage.clear();">
                            <i class="fa-solid fa-right-from-bracket me-2 text-danger"></i>Cerrar sesión
                        </a>
                    </li>
                </ul>
            </div>
        `;
    }
});