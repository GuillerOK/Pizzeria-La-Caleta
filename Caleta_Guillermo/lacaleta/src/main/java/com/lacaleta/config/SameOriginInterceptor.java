package com.lacaleta.config;

import jakarta.servlet.http.*;
import org.springframework.web.servlet.HandlerInterceptor;
import java.net.URI;
import java.util.Set;

/** Rechaza mutaciones iniciadas desde otros sitios con cookies de sesión. */
public class SameOriginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())) return true;
        String origin = request.getHeader("Origin");
        boolean valido = !"cross-site".equals(request.getHeader("Sec-Fetch-Site"));
        if (origin != null) {
            try {
                URI origen = URI.create(origin);
                URI destino = URI.create(request.getRequestURL().toString());
                valido &= origen.getScheme() != null && origen.getHost() != null
                        && origen.getScheme().equalsIgnoreCase(destino.getScheme())
                        && origen.getHost().equalsIgnoreCase(destino.getHost())
                        && puerto(origen) == puerto(destino);
            } catch (IllegalArgumentException error) { valido = false; }
        }
        if (!valido) response.sendError(403, "Origen no permitido");
        return valido;
    }
    private int puerto(URI uri) {
        return uri.getPort() >= 0 ? uri.getPort() : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
