package com.lacaleta.service;

public class CorreoException extends RuntimeException {
    public enum Motivo {
        CONFIGURACION("El servicio de correo no está configurado en el servidor."),
        AUTENTICACION("El servidor de correo rechazó las credenciales del remitente. Revisa la cuenta SMTP y su contraseña de aplicación."),
        CONEXION("No se pudo conectar de forma segura con el servidor de correo. Revisa la conexión y la configuración SMTP."),
        DESTINATARIO("El servidor de correo rechazó la dirección de destino. Revisa el correo ingresado."),
        LIMITE("El proveedor de correo limitó temporalmente los envíos. Inténtalo más tarde."),
        ENVIO("El proveedor de correo rechazó el envío. Revisa el error SMTP en la consola del servidor.");

        private final String mensaje;
        Motivo(String mensaje) { this.mensaje = mensaje; }
    }

    private final Motivo motivo;

    public CorreoException(Motivo motivo, Throwable causa) {
        super(motivo.mensaje, causa);
        this.motivo = motivo;
    }

    public Motivo getMotivo() { return motivo; }
}
