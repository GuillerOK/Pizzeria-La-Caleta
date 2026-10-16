package com.lacaleta.controller;

import com.lacaleta.service.CorreoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice(assignableTypes = PasswordResetController.class)
public class CorreoExceptionHandler {
    @ExceptionHandler(CorreoException.class)
    public ResponseEntity<Map<String, String>> correoNoDisponible(CorreoException error) {
        HttpStatus estado = error.getMotivo() == CorreoException.Motivo.DESTINATARIO
                ? HttpStatus.BAD_REQUEST : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(estado).body(Map.of(
                "error", error.getMessage(), "codigo", "CORREO_" + error.getMotivo().name()));
    }
}
