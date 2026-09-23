package com.eventos.culturales.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

// US1: 5 fallidos → 401; 6º → 429 sin llegar a validar; reset tras ventana y tras éxito.
class LoginRateLimitFilterTest {

    // Reloj mutable para probar la ventana sin Thread.sleep.
    static class MutableClock extends Clock {
        private final AtomicReference<Instant> now;
        MutableClock(Instant now) { this.now = new AtomicReference<>(now); }
        void avanzar(long segundos) { now.updateAndGet(i -> i.plusSeconds(segundos)); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    }

    private MutableClock clock;
    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.now());
        filter = new LoginRateLimitFilter(5, 60, clock);
    }

    private MockHttpServletRequest loginRequest(String email, String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
        req.setServletPath("/auth/login");
        req.setRemoteAddr(ip);
        req.setContentType("application/json");
        req.setContent(("{\"email\":\"" + email + "\",\"password\":\"mala\"}").getBytes());
        return req;
    }

    private static class ContadorChain implements FilterChain {
        final AtomicInteger llamadas = new AtomicInteger();
        final int status;
        ContadorChain(int status) { this.status = status; }
        @Override
        public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
            llamadas.incrementAndGet();
            ((HttpServletResponse) res).setStatus(status);
        }
    }

    @Test
    void cincoFallidos_pasanCon401() throws Exception {
        ContadorChain chain = new ContadorChain(401);
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(loginRequest("admin@test.com", "1.2.3.4"), res, chain);
            assertEquals(401, res.getStatus());
        }
        assertEquals(5, chain.llamadas.get());
    }

    @Test
    void sextoIntento_429_sinValidarCredenciales() throws Exception {
        ContadorChain chain = new ContadorChain(401);
        for (int i = 0; i < 5; i++) {
            filter.doFilter(loginRequest("admin@test.com", "1.2.3.4"), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse bloqueada = new MockHttpServletResponse();
        filter.doFilter(loginRequest("admin@test.com", "1.2.3.4"), bloqueada, chain);
        assertEquals(429, bloqueada.getStatus());
        assertEquals(5, chain.llamadas.get()); // el 6º no llega a validar
    }

    @Test
    void trasLaVentana_contadorReseteado() throws Exception {
        ContadorChain chain = new ContadorChain(401);
        for (int i = 0; i < 6; i++) {
            filter.doFilter(loginRequest("admin@test.com", "1.2.3.4"), new MockHttpServletResponse(), chain);
        }
        assertEquals(5, chain.llamadas.get());

        clock.avanzar(61);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(loginRequest("admin@test.com", "1.2.3.4"), res, chain);
        assertEquals(401, res.getStatus());
        assertEquals(6, chain.llamadas.get());
    }

    @Test
    void loginExitoso_limpiaContador() throws Exception {
        ContadorChain fallos = new ContadorChain(401);
        ContadorChain exito = new ContadorChain(200);
        // 4 fallos (bajo el límite) + 1 éxito que limpia el contador...
        for (int i = 0; i < 4; i++) {
            filter.doFilter(loginRequest("admin@test.com", "1.2.3.4"), new MockHttpServletResponse(), fallos);
        }
        MockHttpServletResponse ok = new MockHttpServletResponse();
        filter.doFilter(loginRequest("admin@test.com", "1.2.3.4"), ok, exito);
        assertEquals(200, ok.getStatus());

        // ...y tras el éxito, 5 fallos más vuelven a pasar sin 429
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(loginRequest("admin@test.com", "1.2.3.4"), res, fallos);
            assertEquals(401, res.getStatus());
        }
        assertEquals(9, fallos.llamadas.get());
    }

    @Test
    void emailsDistintos_noCompartenContador() throws Exception {
        ContadorChain chain = new ContadorChain(401);
        for (int i = 0; i < 5; i++) {
            filter.doFilter(loginRequest("otro@test.com", "1.2.3.4"), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(loginRequest("admin@test.com", "1.2.3.4"), res, chain);
        assertEquals(401, res.getStatus());
        assertEquals(6, chain.llamadas.get());
    }
}
