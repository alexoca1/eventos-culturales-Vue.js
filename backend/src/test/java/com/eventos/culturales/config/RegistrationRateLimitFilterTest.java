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

// 017 US3/FR-004: 5 altas / 60s por IP; la 6ª → 429 sin llegar al controller.
class RegistrationRateLimitFilterTest {

    // Reloj mutable para probar la ventana sin Thread.sleep (igual que en LoginRateLimitFilterTest).
    static class MutableClock extends Clock {
        private final AtomicReference<Instant> now;
        MutableClock(Instant now) { this.now = new AtomicReference<>(now); }
        void avanzar(long segundos) { now.updateAndGet(i -> i.plusSeconds(segundos)); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    }

    private MutableClock clock;
    private RegistrationRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.now());
        filter = new RegistrationRateLimitFilter(5, 60, clock);
    }

    private static MockHttpServletRequest register(String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/register");
        req.setRequestURI("/auth/register");
        req.setRemoteAddr(ip);
        req.setContentType("application/json");
        req.setContent("{\"email\":\"a@test.com\",\"password\":\"Secreto1\"}".getBytes());
        return req;
    }

    private static class ContadorChain implements FilterChain {
        final AtomicInteger llamadas = new AtomicInteger();
        @Override
        public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
            llamadas.incrementAndGet();
            ((HttpServletResponse) res).setStatus(201);
        }
    }

    @Test
    void cincoRegistros_pasan() throws Exception {
        ContadorChain chain = new ContadorChain();
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(register("1.2.3.4"), res, chain);
            assertEquals(201, res.getStatus());
        }
        assertEquals(5, chain.llamadas.get());
    }

    @Test
    void sextoRegistro_429_sinLlegarAlController() throws Exception {
        ContadorChain chain = new ContadorChain();
        for (int i = 0; i < 5; i++) {
            filter.doFilter(register("1.2.3.4"), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse bloqueada = new MockHttpServletResponse();
        filter.doFilter(register("1.2.3.4"), bloqueada, chain);
        assertEquals(429, bloqueada.getStatus());
        assertEquals(5, chain.llamadas.get());
    }

    @Test
    void trasLaVentana_contadorReseteado() throws Exception {
        ContadorChain chain = new ContadorChain();
        for (int i = 0; i < 6; i++) {
            filter.doFilter(register("1.2.3.4"), new MockHttpServletResponse(), chain);
        }
        assertEquals(5, chain.llamadas.get());

        clock.avanzar(61);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(register("1.2.3.4"), res, chain);
        assertEquals(201, res.getStatus());
        assertEquals(6, chain.llamadas.get());
    }

    @Test
    void ipsDistintas_noCompartenContador() throws Exception {
        ContadorChain chain = new ContadorChain();
        for (int i = 0; i < 5; i++) {
            filter.doFilter(register("1.2.3.4"), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse otraIp = new MockHttpServletResponse();
        filter.doFilter(register("5.6.7.8"), otraIp, chain);
        assertEquals(201, otraIp.getStatus());
        assertEquals(6, chain.llamadas.get());
    }

    @Test
    void otrasRutas_oMetodos_noSeFiltran() throws Exception {
        ContadorChain chain = new ContadorChain();

        MockHttpServletRequest login = new MockHttpServletRequest("POST", "/auth/login");
        login.setRequestURI("/auth/login");
        filter.doFilter(login, new MockHttpServletResponse(), chain);

        MockHttpServletRequest get = new MockHttpServletRequest("GET", "/auth/register");
        get.setRequestURI("/auth/register");
        filter.doFilter(get, new MockHttpServletResponse(), chain);

        assertEquals(2, chain.llamadas.get());
    }
}
