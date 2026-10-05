// SPDX-License-Identifier: MIT

package com.eventos.culturales.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Bloquea operaciones destructivas (DELETE) para la cuenta demo.
 * <p>
 * OJO: solo puede vivir dentro de la cadena de seguridad (SecurityConfig). Si se expone
 * como bean de tipo {@code Filter}, Spring Boot lo registra también como filtro del servlet,
 * donde corre ANTES de que exista SecurityContext y no puede saber quién es el usuario.
 */
public class DemoAccountProtectionFilter extends OncePerRequestFilter {

    // Público porque el controlador también lo consulta: 023 permite cambiar el email
    // propio, y la cuenta demo debe seguir siendo la del README (mismo literal, no dos).
    public static final String DEMO_EMAIL = "demo@eventos-culturales.es";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && DEMO_EMAIL.equalsIgnoreCase(emailDe(auth))) {
            if ("DELETE".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\":\"Acción no permitida en modo demostración. Esta cuenta tiene permisos limitados para proteger los datos.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    // El email del usuario. Con JWT el principal es el token, no una cadena, y
    // Authentication.getName() devuelve el toString() del Jwt: hay que leer el claim "sub"
    // (el mismo que usa AuthController para buscar en la BD).
    private static String emailDe(Authentication auth) {
        if (auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            return jwt.getSubject();
        }
        return auth.getName();
    }
}
