package com.lacaleta.config;

import com.lacaleta.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/** Barrera común para las páginas y todas las operaciones administrativas. */
public class AdminInterceptor implements HandlerInterceptor {
    private final com.lacaleta.repository.UsuarioRepository usuarios;
    public AdminInterceptor(com.lacaleta.repository.UsuarioRepository usuarios) { this.usuarios = usuarios; }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        var session = request.getSession(false);
        Usuario usuario = session == null ? null : (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario != null && usuario.esAdmin() && usuario.getId() != null) {
            var actual = usuarios.findById(usuario.getId());
            if (actual.isPresent() && actual.get().esAdmin() && Boolean.TRUE.equals(actual.get().getVerificado())) {
                session.setAttribute("usuarioLogueado", actual.get());
                return true;
            }
        }
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            response.setStatus(usuario == null ? 401 : 403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Se requiere una cuenta de administrador verificada.\"}");
        } else if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
        } else {
            response.sendError(403);
        }
        return false;
    }
}
