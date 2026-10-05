// SPDX-License-Identifier: MIT

package com.eventos.culturales.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Rate limiting para POST /auth/register (017 FR-004): 5 peticiones / 60s por IP.
 * Ventana deslizante en memoria, igual que LoginRateLimitFilter pero sin leer el body
 * (no hace falta clave por email: nos protege de automatización de altas, no de fuerza bruta).
 * Se resetea al reiniciar la app — asumible con una sola instancia (spec 003, Assumptions).
 */
public class RegistrationRateLimitFilter extends OncePerRequestFilter {

    private final int maxAttempts;
    private final long windowSeconds;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public RegistrationRateLimitFilter(int maxAttempts, long windowSeconds, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        // requestURI - contextPath: vale igual en Tomcat (servletPath) y en MockMvc
        // (que lo deja en el URI), así el filtro no queda silenciosamente inactivo en tests.
        String uri = request.getRequestURI();
        String path = uri.substring(request.getContextPath().length());
        return !"/auth/register".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        Deque<Instant> times = attempts.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
        Instant now = clock.instant();
        while (!times.isEmpty() && times.peekFirst().isBefore(now.minusSeconds(windowSeconds))) {
            times.pollFirst();
        }
        if (times.size() >= maxAttempts) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Demasiados intentos de registro. Inténtalo más tarde.\"}");
            return;
        }
        times.addLast(now);

        filterChain.doFilter(request, response);
    }
}
