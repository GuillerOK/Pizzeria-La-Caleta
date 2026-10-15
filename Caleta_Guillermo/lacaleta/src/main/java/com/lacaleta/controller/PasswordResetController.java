package com.lacaleta.controller;

import com.lacaleta.model.Usuario;
import com.lacaleta.repository.UsuarioRepository;
import com.lacaleta.service.EmailService;
import com.lacaleta.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.time.LocalDate;
import java.time.Period;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EmailService emailService;

    // Simula una caché temporal para los OTPs.
    private final ConcurrentHashMap<String, String> otpCache = new ConcurrentHashMap<>();

    // Patrones de validación Regex
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PERU_PATTERN = Pattern.compile("^9\\d{8}$");
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]{2,50}$");

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload, HttpSession session) {
        String email = payload.get("email");
        String password = payload.get("password");

        String emailClean = (email != null) ? email.trim().toLowerCase() : "";
        String passClean = (password != null) ? password.trim() : "";

        Optional<Usuario> usuarioAutenticado = usuarioService.autenticar(emailClean, passClean);

        if (usuarioAutenticado.isPresent()) {
            Usuario usuario = usuarioAutenticado.get();
            session.setAttribute("usuarioLogueado", usuario);

            String redirectUrl = usuario.esAdmin() ? "/admin" : "/";
            return ResponseEntity.ok(Map.of("mensaje", "Inicio de sesión exitoso", "redirect", redirectUrl, "usuario", usuario));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Correo electrónico o contraseña incorrectos."));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        String nombre = payload.get("nombre");
        String apellidos = payload.get("apellidos");
        String tipoDocumento = payload.get("tipoDocumento");
        String numeroDocumento = payload.get("numeroDocumento");
        String telefono = payload.get("telefono");
        String fechaNacimiento = payload.get("fechaNacimiento");
        String email = payload.get("email");
        String password = payload.get("password");
        String confirmPassword = payload.get("confirmPassword");
        String aceptaTerminosStr = payload.get("aceptaTerminos");

        String nomClean = nombre != null ? nombre.trim() : "";
        String apeClean = apellidos != null ? apellidos.trim() : "";
        String tipoDocClean = tipoDocumento != null ? tipoDocumento.trim() : "DNI";
        String docClean = numeroDocumento != null ? numeroDocumento.trim() : "";
        String telClean = telefono != null ? telefono.trim() : "";
        String emailClean = email != null ? email.trim().toLowerCase() : "";
        String passClean = password != null ? password.trim() : "";
        String confirmClean = confirmPassword != null ? confirmPassword.trim() : "";
        boolean aceptaTerminos = "true".equalsIgnoreCase(aceptaTerminosStr);

        if (!NAME_PATTERN.matcher(nomClean).matches() || !NAME_PATTERN.matcher(apeClean).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Los nombres y apellidos deben contener solo letras (mínimo 2 caracteres)."));
        }

        if ("DNI".equalsIgnoreCase(tipoDocClean)) {
            if (!DNI_PATTERN.matcher(docClean).matches()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El DNI debe contener exactamente 8 dígitos numéricos."));
            }
        } else if (docClean.length() < 6 || docClean.length() > 12) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ingresa un número de documento válido (entre 6 y 12 caracteres)."));
        }

        if (!PHONE_PERU_PATTERN.matcher(telClean).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El número de teléfono debe ser un celular peruano válido (9 dígitos comenzando con 9)."));
        }

        if (fechaNacimiento != null && !fechaNacimiento.isBlank()) {
            try {
                LocalDate fechaNac = LocalDate.parse(fechaNacimiento);
                LocalDate hoy = LocalDate.now();

                if (fechaNac.isAfter(hoy)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "La fecha de nacimiento no puede estar en el futuro."));
                }
                int edad = Period.between(fechaNac, hoy).getYears();
                if (edad < 14) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Debes tener al menos 14 años para registrarte."));
                }
                if (edad > 100) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Ingresa una fecha de nacimiento válida."));
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Formato de fecha de nacimiento no válido."));
            }
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Por favor ingresa tu fecha de nacimiento."));
        }

        if (!EMAIL_PATTERN.matcher(emailClean).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ingresa un correo electrónico válido (ejemplo: usuario@dominio.com)."));
        }

        if (passClean.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "La contraseña debe contener al menos 8 caracteres."));
        }

        if (!passClean.equals(confirmClean)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Las contraseñas no coinciden. Por favor verifícalas."));
        }

        if (!aceptaTerminos) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debes aceptar los términos y condiciones para continuar."));
        }

        String nombreCompleto = nomClean + " " + apeClean;
        Usuario nuevoUsuario = new Usuario(null, nombreCompleto, emailClean, passClean, telClean, docClean, 1);

        boolean exito = usuarioService.guardar(nuevoUsuario);

        if (!exito) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo electrónico '" + emailClean + "' ya está registrado."));
        }

        return ResponseEntity.ok(Map.of("mensaje", "¡Registro exitoso! Ya puedes iniciar sesión."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email es requerido."));
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email.toLowerCase());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Usuario no encontrado."));
        }

        // Generar OTP de 6 dígitos
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpCache.put(email.toLowerCase(), otp);

        // Enviar por correo
        boolean enviado = emailService.enviarCodigoRecuperacion(email.toLowerCase(), otp);
        if (enviado) {
            return ResponseEntity.ok(Map.of("mensaje", "Se ha enviado un código de verificación a tu correo."));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al enviar el correo."));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String otp = payload.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan datos requeridos."));
        }

        String storedOtp = otpCache.get(email.toLowerCase());
        if (storedOtp != null && storedOtp.equals(otp)) {
            return ResponseEntity.ok(Map.of("mensaje", "Código verificado con éxito."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "El código ingresado es incorrecto o ha expirado."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String otp = payload.get("otp");
        String newPassword = payload.get("newPassword");

        if (email == null || otp == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan datos requeridos."));
        }

        // 1. Validar OTP
        String storedOtp = otpCache.get(email.toLowerCase());
        if (storedOtp == null || !storedOtp.equals(otp)) {
            return ResponseEntity.badRequest().body(Map.of("error", "El código ingresado es incorrecto o ha expirado."));
        }

        // 2. Validar Regex (mínimo 8 caracteres, 1 mayúscula, 1 carácter especial)
        String strengthRegex = "^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$";
        if (!Pattern.compile(strengthRegex).matcher(newPassword).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La contraseña debe tener mínimo 8 caracteres, al menos 1 mayúscula y 1 carácter especial."));
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email.toLowerCase());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Usuario no encontrado."));
        }

        Usuario usuario = usuarioOpt.get();

        // 3. Verificar que la nueva contraseña no sea igual a la actual
        boolean esIgual = false;
        try {
            if (usuario.getPassword() != null && usuario.getPassword().startsWith("$2a$")) {
                esIgual = BCrypt.checkpw(newPassword, usuario.getPassword());
            } else {
                esIgual = newPassword.equals(usuario.getPassword());
            }
        } catch (Exception e) {
            esIgual = false;
        }

        if (esIgual) {
            return ResponseEntity.badRequest().body(Map.of("error", "La nueva contraseña no puede ser igual a la anterior."));
        }

        // 4. Encriptar y guardar
        String hashedNewPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        usuario.setPassword(hashedNewPassword);
        usuarioRepository.save(usuario);

        // 5. Eliminar OTP
        otpCache.remove(email.toLowerCase());

        return ResponseEntity.ok(Map.of("mensaje", "¡Contraseña restablecida exitosamente!"));
    }
}
