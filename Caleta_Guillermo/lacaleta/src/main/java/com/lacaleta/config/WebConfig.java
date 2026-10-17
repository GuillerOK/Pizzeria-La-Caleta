package com.lacaleta.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final com.lacaleta.repository.UsuarioRepository usuarios;
    public WebConfig(com.lacaleta.repository.UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(new SameOriginInterceptor()).addPathPatterns("/**");
        registry.addInterceptor(new AdminInterceptor(usuarios)).addPathPatterns(
                "/admin", "/admin/**", "/administracion", "/reportes", "/api/admin/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Mapear la ruta virtual /uploads/** al directorio físico uploads/
        // Esto permite servir archivos subidos dinámicamente sin reiniciar el servidor.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
