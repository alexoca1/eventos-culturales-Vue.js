package com.eventos.culturales.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Rate limiting básico para POST /auth/login, sin librerías externas.
 * Cuenta intentos por combinación email+IP en una ventana deslizante:
 * al superar el máximo responde 429 sin llegar a validar credenciales
 * (ahorra los ciclos de BCrypt del AuthenticationManager).
 * Un login con respuesta 200 limpia el contador de esa clave.
 *
 * Estado en memoria (ConcurrentHashMap): se resetea al reiniciar la app,
 * asumible para una sola instancia (ver spec 003, Assumptions).
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final int maxAttempts;
    private final long windowSeconds;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public LoginRateLimitFilter(int maxAttempts, long windowSeconds, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod()) && "/auth/login".equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Leemos el body para extraer el email y lo re-servimos con un wrapper propio.
        // (ContentCachingRequestWrapper NO vale aquí: getInputStream() devuelve siempre
        // el mismo stream y quedaría agotado antes de que Spring lea el body → 500.)
        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        String key = email(body) + "|" + request.getRemoteAddr();

        Deque<Instant> times = attempts.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        Instant now = clock.instant();
        while (!times.isEmpty() && times.peekFirst().isBefore(now.minusSeconds(windowSeconds))) {
            times.pollFirst();
        }
        if (times.size() >= maxAttempts) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Demasiados intentos de login. Espera unos segundos.\"}");
            return;
        }
        times.addLast(now);

        filterChain.doFilter(new CachedBodyRequest(request, body), response);

        if (response.getStatus() == 200) {
            attempts.remove(key);
        }
    }

    private static String email(byte[] body) {
        try {
            String email = MAPPER.readTree(body).path("email").asText(null);
            return email != null ? email : "";
        } catch (Exception e) {
            return "";
        }
    }

    // Wrapper que re-sirve unos bytes ya leídos, tantas veces como se pidan
    private static class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return in.read(); }
                @Override public boolean isFinished() { return in.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) { }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() { return body.length; }

        @Override
        public long getContentLengthLong() { return body.length; }
    }
}
