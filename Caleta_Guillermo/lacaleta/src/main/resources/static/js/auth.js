const API_URL = 'https://pizzeria-la-caleta.onrender.com/api';



function cerrarSesion() {
    localStorage.removeItem("sesion_activa");
    localStorage.removeItem("usuarioLogueado");
    localStorage.removeItem("token");
    window.location.href = "/logout";
}

// Función genérica para mostrar notificaciones con Bootstrap Modal
function mostrarModalMensaje(titulo, mensaje, accionCierre = null) {
    const modalEl = document.getElementById('modalNotificacionGlobal');
    if (!modalEl) {
        // Fallback en caso de que el modal no exista en la vista actual
        alert(`${titulo}: ${mensaje}`);
        if(accionCierre) accionCierre();
        return;
    }
    
    document.getElementById('modalNotificacionGlobalLabel').innerText = titulo;
    document.getElementById('modalNotificacionGlobalMensaje').innerText = mensaje;
    
    const modalInstance = new bootstrap.Modal(modalEl);
    
    if (accionCierre) {
        modalEl.addEventListener('hidden.bs.modal', function onHidden() {
            modalEl.removeEventListener('hidden.bs.modal', onHidden);
            accionCierre();
        });
    }
    
    modalInstance.show();
}

// 3. Ejecución automática al cargar cualquier página
document.addEventListener("DOMContentLoaded", () => {

    // Validaciones preventivas y envío asíncrono para el Registro en Frontend
    const formRegistro = document.getElementById("formRegistro");
    if (formRegistro) {
        formRegistro.addEventListener("submit", async (e) => {
            e.preventDefault();
            
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
            if(errorAlert) errorAlert.classList.add('d-none');

            // Ocultar mensajes previos de error
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

            // Nombres y Apellidos
            if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]{2,50}$/.test(nombre)) {
                marcarErrorCampo('regNombre', 'feedNombre', 'Ingresa un nombre válido (mínimo 2 letras).');
                hayError = true;
            }
            if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]{2,50}$/.test(apellidos)) {
                marcarErrorCampo('regApellido', 'feedApellido', 'Ingresa un apellido válido (mínimo 2 letras).');
                hayError = true;
            }

            // DNI / Documento
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

            // Teléfono Celular
            if (!/^9\d{8}$/.test(telefono)) {
                marcarErrorCampo('regTelefono', 'feedTelefono', 'El celular debe tener 9 dígitos y comenzar con 9.');
                hayError = true;
            }

            // Fecha de Nacimiento
            if (!fechaNac) {
                marcarErrorCampo('regFechaNac', 'feedFechaNac', 'Por favor selecciona tu fecha de nacimiento.');
                hayError = true;
            }

            // Correo Electrónico
            if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[a-zA-Z]{2,}$/.test(email)) {
                marcarErrorCampo('regEmail', 'feedEmail', 'Ingresa un correo electrónico válido.');
                hayError = true;
            }

            // Contraseñas
            if (pass.length < 8) {
                marcarErrorCampo('regPassword', 'feedPassword', 'La contraseña debe tener al menos 8 caracteres.');
                hayError = true;
            }
            if (pass !== confirmPass) {
                marcarErrorCampo('regPasswordConfirm', 'feedPasswordConfirm', 'Las contraseñas no coinciden.');
                hayError = true;
            }

            // Términos
            if (aceptaTerminos !== "true") {
                marcarErrorCampo('checkTerminos', 'feedTerminos', 'Debes aceptar los términos y condiciones.');
                hayError = true;
            }

            if (hayError) {
                if(errorAlert) {
                    errorAlert.innerText = 'Por favor corrige los campos marcados en rojo antes de continuar.';
                    errorAlert.classList.remove('d-none');
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                }
                return;
            }
            
            // Si pasa las validaciones previas, hace el fetch
            try {
                if (btnSubmit) {
                    btnSubmit.disabled = true;
                    if(btnSubmit.tagName === 'INPUT') btnSubmit.value = "REGISTRANDO...";
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
                
                const data = await response.json();
                
                if (response.ok) {
                    mostrarModalMensaje("Éxito", data.mensaje, () => {
                        window.location.href = "/login";
                    });
                } else {
                    mostrarModalMensaje("Error", data.error || "Ocurrió un error en el registro.");
                }
            } catch (error) {
                mostrarModalMensaje("Error", "No se pudo conectar con el servidor.");
            } finally {
                if (btnSubmit) {
                    btnSubmit.disabled = false;
                    if(btnSubmit.tagName === 'INPUT') btnSubmit.value = btnOriginalText;
                    else btnSubmit.innerText = btnOriginalText;
                }
            }
        });
    }

    // --- FLUJO: INICIAR SESIÓN (Fetch Async) ---
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
                        if(btnSubmit.tagName === 'INPUT') btnSubmit.value = "INICIANDO SESIÓN...";
                        else btnSubmit.innerText = "INICIANDO SESIÓN...";
                    }
                    
                    const response = await fetch('/api/auth/login', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ email, password })
                    });
                    
                    const data = await response.json();
                    
                    if (response.ok) {
                        // Guardar datos en localstorage si es necesario
                        localStorage.setItem("sesion_activa", JSON.stringify(data.usuario));
                        window.location.href = data.redirect;
                    } else {
                        mostrarModalMensaje("Error", data.error || "Credenciales incorrectas.");
                    }
                } catch (error) {
                    mostrarModalMensaje("Error", "No se pudo conectar con el servidor.");
                } finally {
                    if (btnSubmit) {
                        btnSubmit.disabled = false;
                        if(btnSubmit.tagName === 'INPUT') btnSubmit.value = btnOriginalText;
                        else btnSubmit.innerText = btnOriginalText;
                    }
                }
            }
        });
    }

    // Toggle ver contraseña en Login
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

    // Toggle ver contraseña (reset password 1)
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

    // Toggle ver contraseña (reset password 2)
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

    // --- FLUJO: OLVIDÉ MI CONTRASEÑA ---
    const formForgotPassword = document.getElementById("formForgotPassword");
    if (formForgotPassword) {
        formForgotPassword.addEventListener("submit", async (e) => {
            e.preventDefault();
            const emailInput = document.getElementById("forgotEmail");
            const btnSubmit = formForgotPassword.querySelector("input[type='submit']");
            
            if (emailInput && emailInput.value.trim() !== "") {
                const email = emailInput.value.trim().toLowerCase();
                
                try {
                    if (btnSubmit) {
                        btnSubmit.disabled = true;
                        btnSubmit.value = "ENVIANDO...";
                    }
                    
                    const response = await fetch('/api/auth/forgot-password', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ email })
                    });
                    
                    const data = await response.json();
                    
                    if (response.ok) {
                        localStorage.setItem("temp_reset_email", email);
                        mostrarModalMensaje("Éxito", data.mensaje, () => {
                            window.location.href = "/verifycode";
                        });
                    } else {
                        mostrarModalMensaje("Error", data.error || "Ocurrió un error inesperado.");
                    }
                } catch (error) {
                    mostrarModalMensaje("Error", "No se pudo conectar con el servidor.");
                } finally {
                    if (btnSubmit) {
                        btnSubmit.disabled = false;
                        btnSubmit.value = "RESTABLECER MI CONTRASEÑA";
                    }
                }
            }
        });
    }

    const formVerifyCode = document.getElementById("formVerifyCode");
    if (formVerifyCode) {
        formVerifyCode.addEventListener("submit", async (e) => {
            e.preventDefault();
            const inputOtp = document.getElementById("otpCode")?.value.trim();
            const email = localStorage.getItem("temp_reset_email");
            const btnSubmit = formVerifyCode.querySelector("input[type='submit']");

            if (!email) {
                mostrarModalMensaje("Error", "No hay una sesión de recuperación activa. Vuelve a empezar.", () => {
                    window.location.href = "/forgotpassword";
                });
                return;
            }

            try {
                if (btnSubmit) {
                    btnSubmit.disabled = true;
                    btnSubmit.value = "VERIFICANDO...";
                }
                
                const response = await fetch('/api/auth/verify-otp', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, otp: inputOtp })
                });
                
                const data = await response.json();
                
                if (response.ok) {
                    localStorage.setItem("temp_verified_otp", inputOtp);
                    mostrarModalMensaje("Éxito", data.mensaje, () => {
                        window.location.href = "/resetpassword";
                    });
                } else {
                    mostrarModalMensaje("Error", data.error || "El código ingresado es incorrecto.");
                }
            } catch (error) {
                mostrarModalMensaje("Error", "No se pudo conectar con el servidor.");
            } finally {
                if (btnSubmit) {
                    btnSubmit.disabled = false;
                    btnSubmit.value = "VERIFICAR CÓDIGO";
                }
            }
        });
    }

    const formResetPassword = document.getElementById("formResetPassword");
    if (formResetPassword) {
        formResetPassword.addEventListener("submit", async (e) => {
            e.preventDefault();
            const newPassword = document.getElementById("newPassword")?.value || "";
            const confirmPassword = document.getElementById("confirmPassword")?.value || "";
            const email = localStorage.getItem("temp_reset_email");
            const otp = localStorage.getItem("temp_verified_otp");
            const btnSubmit = formResetPassword.querySelector("input[type='submit']");

            if (!email || !otp) {
                mostrarModalMensaje("Error", "Faltan datos de verificación. Vuelve a iniciar el proceso.", () => {
                    window.location.href = "/forgotpassword";
                });
                return;
            }

            if (newPassword !== confirmPassword) {
                mostrarModalMensaje("Error", "Las contraseñas no coinciden.");
                return;
            }

            const strengthRegex = /^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$/;
            if (!strengthRegex.test(newPassword)) {
                mostrarModalMensaje("Error", "La contraseña debe tener mínimo 8 caracteres, al menos 1 mayúscula y 1 carácter especial.");
                return;
            }

            try {
                if (btnSubmit) {
                    btnSubmit.disabled = true;
                    btnSubmit.value = "ACTUALIZANDO...";
                }
                
                const response = await fetch('/api/auth/reset-password', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, otp, newPassword })
                });
                
                const data = await response.json();
                
                if (response.ok) {
                    localStorage.removeItem("temp_reset_email");
                    localStorage.removeItem("temp_verified_otp");
                    
                    mostrarModalMensaje("Éxito", data.mensaje, () => {
                        window.location.href = "/login";
                    });
                } else {
                    mostrarModalMensaje("Error", data.error || "No se pudo restablecer la contraseña.");
                }
            } catch (error) {
                mostrarModalMensaje("Error", "No se pudo conectar con el servidor.");
            } finally {
                if (btnSubmit) {
                    btnSubmit.disabled = false;
                    btnSubmit.value = "CONFIRMAR CONTRASEÑA";
                }
            }
        });
    }
});