package com.lacaleta.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

public final class ImagenProductoStorage {
    private ImagenProductoStorage() { }
    public static String guardar(MultipartFile archivo) throws IOException {
        if (archivo.getSize() > 2 * 1024 * 1024) throw new IOException("Imagen demasiado grande");
        byte[] contenido = archivo.getBytes();
        String extension;
        if (contenido.length > 8 && contenido[0] == (byte) 0x89 && contenido[1] == 'P'
                && contenido[2] == 'N' && contenido[3] == 'G') extension = ".png";
        else if (contenido.length > 3 && contenido[0] == (byte) 0xff && contenido[1] == (byte) 0xd8
                && contenido[2] == (byte) 0xff) extension = ".jpg";
        else throw new IOException("Solo se permiten imágenes PNG o JPEG");
        Path carpeta = Path.of("uploads", "pizzas").toAbsolutePath().normalize();
        Files.createDirectories(carpeta);
        String nombre = UUID.randomUUID() + extension;
        Files.write(carpeta.resolve(nombre), contenido, StandardOpenOption.CREATE_NEW);
        return "/uploads/pizzas/" + nombre;
    }
    public static boolean urlValida(String valor) {
        if (valor == null || valor.isBlank()) return true;
        String url = valor.trim();
        return !url.matches(".*[\\s\"'<>].*") && (url.startsWith("https://")
                || url.startsWith("/images/") || url.startsWith("/uploads/pizzas/"));
    }
}
