const API_URL = 'https://pizzeria-la-caleta.onrender.com/api';

function cerrarSesion() {
    localStorage.removeItem("sesion_activa");
    localStorage.removeItem("usuarioLogueado");
    localStorage.removeItem("token");
    window.location.href = "/logout";
}

// Función genérica para mostrar notificaciones con Bootstrap Modal
function mostrarModalMensaje(titulo, mensaje, accionCierre = null) {
    let modalEl = document.getElementById('modalNotificacionGlobal');
    
    // Si la plantilla no incluyó el fragmento, lo inyectamos dinámicamente en el DOM
    if (!modalEl) {
        modalEl = document.createElement('div');
        modalEl.className = 'modal fade';
        modalEl.id = 'modalNotificacionGlobal';
        modalEl.tabIndex = -1;
        modalEl.setAttribute('aria-labelledby', 'modalNotificacionGlobalLabel');
        modalEl.setAttribute('aria-hidden', 'true');
        modalEl.innerHTML = `
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content text-white border border-secondary border-opacity-25"
                    style="background-color: #232323; border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.5);">
                    <div class="modal-header border-bottom border-secondary border-opacity-25 py-3">
                        <h5 class="modal-title fw-bold fs-5 text-white mb-0 d-flex align-items-center gap-2" id="modalNotificacionGlobalLabel"></h5>
                        <button type="button" class="btn-close btn-close-white shadow-none" data-bs-dismiss="modal" aria-label="Cerrar"></button>
                    </div>
                    <div class="modal-body py-4">
                        <p class="mb-0 text-white-50 fs-6" id="modalNotificacionGlobalMensaje"></p>
                    </div>
                    <div class="modal-footer border-top border-secondary border-opacity-25 py-2">
                        <button type="button" class="btn btn-secondary px-4 fw-bold text-uppercase small" data-bs-dismiss="modal" style="border-radius: 30px;">
                            Cerrar
                        </button>
                    </div>
                </div>
            </div>
        `;
        document.body.appendChild(modalEl);
    }
    
    const labelEl = document.getElementById('modalNotificacionGlobalLabel');
    const msgEl = document.getElementById('modalNotificacionGlobalMensaje');
    
    let iconoHtml = '<i class="fa-solid fa-circle-info text-info me-2"></i>';
    const tituloLower = (titulo || '').toLowerCase();
    if (tituloLower.includes('error')) {
        iconoHtml = '<i class="fa-solid fa-circle-xmark text-danger me-2"></i>';
    } else if (tituloLower.includes('éxito') || tituloLower.includes('exito')) {
        iconoHtml = '<i class="fa-solid fa-circle-check text-success me-2"></i>';
    }
    
    if (labelEl) labelEl.innerHTML = `${iconoHtml}<span>${titulo || 'Notificación'}</span>`;
    if (msgEl) msgEl.innerText = mensaje || '';
    
    const modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);
    
    if (accionCierre) {
        const onHidden = function () {
            modalEl.removeEventListener('hidden.bs.modal', onHidden);
            accionCierre();
        };
        modalEl.addEventListener('hidden.bs.modal', onHidden);
    }
    
    modalInstance.show();
}

// Sincronizar estado de sesión en el Navbar (Sobreescribe para incluir 'Mi Perfil' siempre)
function actualizarNavbar() {
    const sesion = JSON.parse(localStorage.getItem("sesion_activa") || localStorage.getItem("usuarioLogueado"));
    const navUserArea = document.getElementById("navUserArea");

    if (!navUserArea) return;

    if (sesion && sesion.nombre) {
        const esAdmin = Number(sesion.rolId) === 2;
        const iconoRol = esAdmin ? "fa-user-shield text-warning" : "fa-user-check text-success";
        
        navUserArea.innerHTML = `
            <div class="dropdown">
                <button class="btn btn-dark text-white dropdown-toggle border-0 small fw-semibold d-flex align-items-center gap-2 shadow-none" 
                        type="button" data-bs-toggle="dropdown" aria-expanded="false">
                    <i class="fa-solid ${iconoRol} fs-5"></i>
                    <span>${sesion.nombre}</span>
                </button>
                <ul class="dropdown-menu dropdown-menu-end bg-dark border-secondary shadow">
                    <li>
                        <a class="dropdown-item text-white small" href="/perfil">
                            <i class="fa-solid fa-user me-2 text-danger"></i>Mi Perfil & Pedidos
                        </a>
                    </li>
                    ${esAdmin ? '<li><a class="dropdown-item text-warning small fw-bold" href="/admin"><i class="fa-solid fa-gauge me-2"></i>Panel Admin</a></li>' : ''}
                    <li><hr class="dropdown-divider border-secondary"></li>
                    <li>
                        <a class="dropdown-item text-white small" href="#" onclick="cerrarSesion()">
                            <i class="fa-solid fa-right-from-bracket me-2 text-danger"></i>Cerrar sesión
                        </a>
                    </li>
                </ul>
            </div>
        `;
    }
}

document.addEventListener("DOMContentLoaded", () => {
    actualizarNavbar();
    const flash = document.getElementById('notificacionFlash');
    if (flash) mostrarModalMensaje(flash.dataset.titulo, flash.dataset.mensaje);

    // -------------------------------------------------------------------------
    // 1. REGISTRO DE USUARIO (formRegistro)
    // -------------------------------------------------------------------------
    const formRegistro = document.getElementById("formRegistro");
    const modalCuentaEl = document.getElementById('modalVerificacionCuenta');
    const formVerificacionCuenta = document.getElementById('formVerificacionCuenta');
    let emailCuentaPendiente = '';
    let registrando = false;
    let verificandoCuenta = false;

    if (modalCuentaEl && formVerificacionCuenta) {
        const otpInput = document.getElementById('otpVerificacionCuenta');
        const errorCuenta = document.getElementById('errorVerificacionCuenta');
        modalCuentaEl.addEventListener('shown.bs.modal', () => otpInput.focus());
        modalCuentaEl.addEventListener('hide.bs.modal', (event) => {
            if (verificandoCuenta) event.preventDefault();
        });
        formVerificacionCuenta.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (verificandoCuenta || !formVerificacionCuenta.reportValidity()) return;
            const btn = formVerificacionCuenta.querySelector('button[type="submit"]');
            verificandoCuenta = true;
            btn.disabled = true;
            btn.textContent = 'Verificando...';
            errorCuenta.classList.add('d-none');
            try {
                const response = await fetch('/api/auth/verify-account', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email: emailCuentaPendiente, otp: otpInput.value.trim() })
                });
                const data = await response.json().catch(() => ({}));
                if (!response.ok) {
                    errorCuenta.textContent = data.error || 'No se pudo verificar el código. Inténtalo de nuevo.';
                    errorCuenta.classList.remove('d-none');
                    return;
                }
                verificandoCuenta = false;
                modalCuentaEl.addEventListener('hidden.bs.modal', () => {
                    mostrarModalMensaje('¡Bienvenido!', data.mensaje || 'Tu cuenta está verificada.', () => {
                        redirigirA('login.html');
                    });
                }, { once: true });
                bootstrap.Modal.getOrCreateInstance(modalCuentaEl).hide();
            } catch (error) {
                errorCuenta.textContent = 'No se pudo conectar con el servidor. Vuelve a intentarlo.';
                errorCuenta.classList.remove('d-none');
            } finally {
                verificandoCuenta = false;
                btn.disabled = false;
                btn.textContent = 'Verificar cuenta';
            }
        });
    }

    if (formRegistro) {
        formRegistro.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (registrando) return;
            
            const btnSubmit = formRegistro.querySelector("button[type='submit']") || formRegistro.querySelector("input[type='submit']");
            const btnOriginalText = btnSubmit ? (btnSubmit.innerText || btnSubmit.value) : "CREAR CUENTA";

            const nombre = document.getElementById("regNombre")?.value.trim() || "";
            const apellidos = document.getElementById("regApellido")?.value.trim() || "";
            const tipoDoc = document.getElementById("regTipoDoc")?.value;
            const numDoc = document.getElementById("regNumDoc")?.value.trim() || "";
            const telefono = document.getElementById("regTelefono")?.value.trim() || "";
            const fechaNac = document.getElementById("regFechaNac")?.value || "";
            const email = document.getElementById("regEmail")?.value.trim() || "";
            const pass = document.getElementById("regPassword")?.value || "";
            const confirmPass = document.getElementById("regPasswordConfirm")?.value || "";
            const aceptaTerminos = document.getElementById("checkTerminos")?.checked ? "true" : "false";

            const errorAlert = document.getElementById('jsErrorAlert');
            if (errorAlert) errorAlert.classList.add('d-none');

            document.querySelectorAll('.msg-feedback').forEach(el => el.classList.add('d-none'));
            document.querySelectorAll('input').forEach(el => el.classList.remove('is-invalid-custom'));

            function marcarErrorCampo(idInput, idFeed, msg) {
                const input = document.getElementById(idInput);
                const feed = document.getElementById(idFeed);
                if (input) input.classList.add('is-invalid-custom');
                if (feed) {
                    feed.innerText = msg;
                    feed.className = 'msg-feedback text-danger fw-semibold';
                    feed.classList.remove('d-none');
                }
            }

            let hayError = false;

            if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]{2,50}$/.test(nombre)) {
                marcarErrorCampo('regNombre', 'feedNombre', 'Ingresa un nombre válido (mínimo 2 letras).');
                hayError = true;
            }
            if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]{2,50}$/.test(apellidos)) {
                marcarErrorCampo('regApellido', 'feedApellido', 'Ingresa un apellido válido (mínimo 2 letras).');
                hayError = true;
            }

            if (tipoDoc === 'DNI') {
                if (!/^\d{8}$/.test(numDoc)) {
                    marcarErrorCampo('regNumDoc', 'feedNumDoc', 'El DNI debe tener exactamente 8 dígitos.');
                    hayError = true;
                } else if (/^(\d)\1{7}$/.test(numDoc) || numDoc === '12345678' || numDoc === '87654321') {
                    marcarErrorCampo('regNumDoc', 'feedNumDoc', 'Por favor ingresa un número de DNI válido.');
                    hayError = true;
                }
            } else if (numDoc.length < 6 || numDoc.length > 12) {
                marcarErrorCampo('regNumDoc', 'feedNumDoc', 'El documento debe tener entre 6 y 12 caracteres.');
                hayError = true;
            }

            if (!/^9\d{8}$/.test(telefono)) {
                marcarErrorCampo('regTelefono', 'feedTelefono', 'El celular debe tener 9 dígitos y comenzar con 9.');
                hayError = true;
            }

            if (!fechaNac) {
                marcarErrorCampo('regFechaNac', 'feedFechaNac', 'Por favor selecciona tu fecha de nacimiento.');
                hayError = true;
            }

            if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[a-zA-Z]{2,}$/.test(email)) {
                marcarErrorCampo('regEmail', 'feedEmail', 'Ingresa un correo electrónico válido.');
                hayError = true;
            }

            if (pass.length < 8) {
                marcarErrorCampo('regPassword', 'feedPassword', 'La contraseña debe tener al menos 8 caracteres.');
                hayError = true;
            }
            if (pass !== confirmPass) {
                marcarErrorCampo('regPasswordConfirm', 'feedPasswordConfirm', 'Las contraseñas no coinciden.');
                hayError = true;
            }

            if (aceptaTerminos !== "true") {
                marcarErrorCampo('checkTerminos', 'feedTerminos', 'Debes aceptar los términos y condiciones.');
                hayError = true;
            }

            if (hayError) {
                mostrarModalMensaje('Error', 'Por favor corrige los campos marcados en rojo antes de continuar.');
                return;
            }
            
            try {
                registrando = true;
                if (btnSubmit) {
                    btnSubmit.disabled = true;
                    if (btnSubmit.tagName === 'INPUT') btnSubmit.value = "REGISTRANDO...";
                    else btnSubmit.innerText = "REGISTRANDO...";
                }
                
                const response = await fetch('/api/auth/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ 
                        nombre, apellidos, tipoDocumento: tipoDoc, numeroDocumento: numDoc, 
                        telefono, fechaNacimiento: fechaNac, email, password: pass, 
                        confirmPassword: confirmPass, aceptaTerminos 
                    })
                });
                
                const data = await response.json().catch(() => ({}));
                
                if (response.ok) {
                    emailCuentaPendiente = data.email || email.toLowerCase();
                    document.getElementById('correoVerificacionCuenta').textContent = emailCuentaPendiente;
                    formVerificacionCuenta.reset();
                    document.getElementById('errorVerificacionCuenta').classList.add('d-none');
                    bootstrap.Modal.getOrCreateInstance(modalCuentaEl).show();
                } else {
                    mostrarModalMensaje("Error", data.error || "Ocurrió un error en el registro.");
                }
            } catch (error) {
                mostrarModalMensaje("Error", "No se pudo conectar con el servidor.");
            } finally {
                registrando = false;
                if (btnSubmit) {
                    btnSubmit.disabled = false;
                    if (btnSubmit.tagName === 'INPUT') btnSubmit.value = btnOriginalText;
                    else btnSubmit.innerText = btnOriginalText;
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // 2. INICIO DE SESIÓN (formLogin)
    // -------------------------------------------------------------------------
    const formLogin = document.getElementById("formLogin");
    if (formLogin) {
        formLogin.addEventListener("submit", async (e) => {
            e.preventDefault();
            const emailInput = document.getElementById("loginEmail");
            const passInput = document.getElementById("loginPassword");
            const btnSubmit = formLogin.querySelector("button[type='submit']") || formLogin.querySelector("input[type='submit']");
            const btnOriginalText = btnSubmit ? (btnSubmit.innerText || btnSubmit.value) : "INICIAR SESIÓN";

            if (emailInput && passInput && emailInput.value.trim() !== "" && passInput.value.trim() !== "") {
                const email = emailInput.value.trim().toLowerCase();
                const password = passInput.value;
                
                try {
                    if (btnSubmit) {
                        btnSubmit.disabled = true;
                        if (btnSubmit.tagName === 'INPUT') btnSubmit.value = "INICIANDO SESIÓN...";
                        else btnSubmit.innerText = "INICIANDO SESIÓN...";
                    }
                    
                    const response = await fetch('/api/auth/login', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ email, password })
                    });
                    
                    const data = await response.json();
                    
                    if (response.ok) {
                        localStorage.setItem("sesion_activa", JSON.stringify(data.usuario));
                        window.location.href = data.redirect || "/";
                    } else {
                        mostrarModalMensaje("Error", data.error || "Credenciales incorrectas.");
                    }
                } catch (error) {
                    mostrarModalMensaje("Error", "No se pudo conectar con el servidor.");
                } finally {
                    if (btnSubmit) {
                        btnSubmit.disabled = false;
                        if (btnSubmit.tagName === 'INPUT') btnSubmit.value = btnOriginalText;
                        else btnSubmit.innerText = btnOriginalText;
                    }
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // 3. ALTERNAR VISIBILIDAD DE CONTRASEÑA
    // -------------------------------------------------------------------------
    const toggleEye = document.getElementById("toggleEye");
    if (toggleEye) {
        toggleEye.addEventListener("click", () => {
            const passInput = document.getElementById("loginPassword");
            if (passInput) {
                passInput.type = passInput.type === "password" ? "text" : "password";
                toggleEye.classList.toggle("fa-eye");
                toggleEye.classList.toggle("fa-eye-slash");
            }
        });
    }

    const toggleEye1 = document.getElementById("toggleEye1");
    if (toggleEye1) {
        toggleEye1.addEventListener("click", () => {
            const passInput = document.getElementById("newPassword");
            if (passInput) {
                passInput.type = passInput.type === "password" ? "text" : "password";
                toggleEye1.classList.toggle("fa-eye");
                toggleEye1.classList.toggle("fa-eye-slash");
            }
        });
    }

    const toggleEye2 = document.getElementById("toggleEye2");
    if (toggleEye2) {
        toggleEye2.addEventListener("click", () => {
            const passInput = document.getElementById("confirmPassword");
            if (passInput) {
                passInput.type = passInput.type === "password" ? "text" : "password";
                toggleEye2.classList.toggle("fa-eye");
                toggleEye2.classList.toggle("fa-eye-slash");
            }
        });
    }

    // -------------------------------------------------------------------------
    // 4. RECUPERACIÓN DE CONTRASEÑA MEDIANTE LA API Y CORREO SMTP
    // -------------------------------------------------------------------------

    // Función auxiliar para redirigir compatible tanto en archivos estáticos (.html) como en servidor (/ruta)
    const redirigirA = (pagina) => {
        const esArchivoEstatico = window.location.protocol === "file:" || window.location.pathname.endsWith(".html");
        if (esArchivoEstatico) {
            window.location.href = pagina;
        } else {
            const rutaLimpia = pagina.replace(".html", "");
            window.location.href = rutaLimpia.startsWith("/") ? rutaLimpia : "/" + rutaLimpia;
        }
    };

    // Solo se conserva en esta pestaña el correo y el código introducido por el usuario.
    // La API comprueba siempre el OTP; el navegador nunca genera ni decide su validez.
    async function solicitarRecuperacion(form, ruta, payload, texto, alCompletar) {
        if (form.dataset.enviando === 'true') return;
        const boton = form.querySelector('[type="submit"]');
        const original = boton?.tagName === 'INPUT' ? boton.value : boton?.textContent;
        form.dataset.enviando = 'true';
        if (boton) {
            boton.disabled = true;
            if (boton.tagName === 'INPUT') boton.value = texto;
            else boton.textContent = texto;
        }
        try {
            const response = await fetch(`/api/auth/${ruta}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await response.json().catch(() => ({}));
            if (!response.ok) {
                mostrarModalMensaje('Error', data.error || 'No se pudo completar la solicitud. Inténtalo de nuevo.');
                return;
            }
            alCompletar(data);
        } catch (error) {
            mostrarModalMensaje('Error', 'No se pudo conectar con el servidor. Vuelve a intentarlo.');
        } finally {
            form.dataset.enviando = 'false';
            if (boton) {
                boton.disabled = false;
                if (boton.tagName === 'INPUT') boton.value = original;
                else boton.textContent = original;
            }
        }
    }

    const formForgotPassword = document.getElementById("formForgotPassword");
    if (formForgotPassword) {
        formForgotPassword.addEventListener("submit", async (e) => {
            e.preventDefault();

            // Capturar el correo electrónico ingresado
            const emailInput = document.getElementById("forgotEmail");
            const email = emailInput ? emailInput.value.trim().toLowerCase() : "";

            if (!email) {
                mostrarModalMensaje("Error", "Por favor, ingresa tu correo electrónico.");
                return;
            }

            await solicitarRecuperacion(formForgotPassword, 'forgot-password', { email }, 'Enviando...', (data) => {
                sessionStorage.setItem('reset_email', email);
                sessionStorage.removeItem('reset_otp');
                // Retirar los datos que utilizaba la versión anterior del formulario.
                ['temp_reset_email', 'temp_otp_code', 'temp_verified_otp'].forEach(key => localStorage.removeItem(key));
                mostrarModalMensaje('Código de recuperación', data.mensaje || 'Revisa el código enviado a tu correo.', () => {
                    redirigirA('verifycode.html');
                });
            });
        });
    }

    // Flujo 2: Verificar Código (formVerifyCode)
    const formVerifyCode = document.getElementById("formVerifyCode");
    if (formVerifyCode) {
        formVerifyCode.addEventListener("submit", async (e) => {
            e.preventDefault();

            const otpInput = document.getElementById("otpCode");
            const inputOtp = otpInput ? otpInput.value.trim() : "";
            const email = sessionStorage.getItem('reset_email');

            if (!email) {
                mostrarModalMensaje("Error", "No existe un código de recuperación activo. Por favor, solicita uno nuevo.", () => {
                    redirigirA("forgotpassword.html");
                });
                return;
            }

            if (!/^[0-9]{6}$/.test(inputOtp)) {
                mostrarModalMensaje('Error', 'Ingresa el código de 6 dígitos que recibiste por correo.');
                return;
            }
            sessionStorage.removeItem('reset_otp');
            await solicitarRecuperacion(formVerifyCode, 'verify-otp', { email, otp: inputOtp }, 'Verificando...', (data) => {
                sessionStorage.setItem('reset_otp', inputOtp);
                mostrarModalMensaje('Éxito', data.mensaje || 'Código verificado.', () => {
                    redirigirA('resetpassword.html');
                });
            });
        });
    }

    const reenviarCodigo = document.getElementById('reenviarCodigoRecuperacion');
    if (reenviarCodigo && formVerifyCode) {
        reenviarCodigo.addEventListener('click', async (e) => {
            e.preventDefault();
            const email = sessionStorage.getItem('reset_email');
            if (!email) {
                mostrarModalMensaje('Error', 'Solicita un código con tu correo.', () => redirigirA('forgotpassword.html'));
                return;
            }
            await solicitarRecuperacion(formVerifyCode, 'forgot-password', { email }, 'Reenviando...', (data) => {
                sessionStorage.removeItem('reset_otp');
                mostrarModalMensaje('Código de recuperación', data.mensaje || 'Revisa tu correo.');
            });
        });
    }

    // Flujo 3: Restablecer Contraseña (formResetPassword)
    const formResetPassword = document.getElementById("formResetPassword");
    if (formResetPassword) {
        formResetPassword.addEventListener("submit", async (e) => {
            e.preventDefault();

            // Capturar los campos de "nueva contraseña" y "confirmar contraseña"
            const newPassword = document.getElementById("newPassword")?.value || "";
            const confirmPassword = document.getElementById("confirmPassword")?.value || "";

            const email = sessionStorage.getItem('reset_email');
            const otp = sessionStorage.getItem('reset_otp');
            if (!email || !otp) {
                mostrarModalMensaje('Error', 'Primero verifica el código enviado a tu correo.', () => redirigirA('forgotpassword.html'));
                return;
            }

            // Regla 1 (Igualdad): Validar que la nueva contraseña y la confirmación coincidan exactamente
            if (newPassword !== confirmPassword) {
                mostrarModalMensaje("Error", "Las contraseñas no coinciden. Asegúrate de que ambas sean idénticas.");
                return;
            }

            // Regla 2 (Fuerza): Validar mediante Regex (mínimo 8 caracteres, al menos 1 mayúscula y al menos 1 carácter especial)
            const regexFuerza = /^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$/;
            if (!regexFuerza.test(newPassword)) {
                mostrarModalMensaje("Error", "La contraseña debe tener mínimo 8 caracteres, al menos 1 letra mayúscula y al menos 1 carácter especial.");
                return;
            }

            await solicitarRecuperacion(formResetPassword, 'reset-password', { email, otp, newPassword }, 'Guardando...', (data) => {
                sessionStorage.removeItem('reset_email');
                sessionStorage.removeItem('reset_otp');
                mostrarModalMensaje('Éxito', data.mensaje || 'Tu contraseña ha sido restablecida.', () => {
                    redirigirA('login.html');
                });
            });
        });
    }
});
