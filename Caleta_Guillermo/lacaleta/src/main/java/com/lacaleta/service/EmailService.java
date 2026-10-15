package com.lacaleta.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public boolean enviarCodigoRecuperacion(String destino, String codigo) {
        try {
            if (mailSender == null) {
                // Fallback para pruebas locales si aún no configuras SMTP
                System.out.println(">>> CÓDIGO DE RECUPERACIÓN PARA " + destino + ": " + codigo);
                return true;
            }

            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destino);
            mensaje.setSubject("La Caleta - Código de Recuperación de Contraseña");
            mensaje.setText("Hola,\n\nTu código de verificación para restablecer tu contraseña en La Caleta es: " 
                            + codigo + "\n\nEste código vencerá en 10 minutos.\n\nSi no solicitaste este cambio, ignora este mensaje.");
            
            mailSender.send(mensaje);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}