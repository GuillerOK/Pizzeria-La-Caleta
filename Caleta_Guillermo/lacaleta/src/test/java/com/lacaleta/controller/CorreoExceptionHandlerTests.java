package com.lacaleta.controller;

import com.lacaleta.service.CorreoException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;

class CorreoExceptionHandlerTests {
    @Test
    void respuestaPublicaIncluyeCausaUtilPeroNoDetallesSmtp() {
        CorreoException error = new CorreoException(CorreoException.Motivo.AUTENTICACION,
                new IllegalStateException("dato privado"));
        var respuesta = new CorreoExceptionHandler().correoNoDisponible(error);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, respuesta.getStatusCode());
        assertEquals("CORREO_AUTENTICACION", respuesta.getBody().get("codigo"));
        assertFalse(respuesta.getBody().toString().contains("dato privado"));
    }
}
