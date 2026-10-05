package com.eventos.culturales.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 019 FR-002: la cuenta demo no puede ejecutar ningún DELETE.
class DemoAccountProtectionFilterTest {

    private final DemoAccountProtectionFilter filtro = new DemoAccountProtectionFilter();

    private MockFilterChain cadena;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deleteDeLaCuentaDemo_403_yNoLlegaAlDestino() throws Exception {
        autenticar("demo@eventos-culturales.es");
        MockHttpServletResponse response = filtrar("DELETE", "/eventos/1");

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("modo demostración"));
        assertNull(cadena.getRequest(), "la cadena no debe continuar hasta el controller");
    }

    @Test
    void deleteDeOtroUsuario_pasa() throws Exception {
        autenticar("admin@test.com");
        MockHttpServletResponse response = filtrar("DELETE", "/eventos/1");

        assertEquals(200, response.getStatus());
        assertNotNull(cadena.getRequest());
    }

    @Test
    void getDeLaCuentaDemo_pasa() throws Exception {
        autenticar("demo@eventos-culturales.es");
        MockHttpServletResponse response = filtrar("GET", "/eventos/1");

        assertEquals(200, response.getStatus());
        assertNotNull(cadena.getRequest());
    }

    private MockHttpServletResponse filtrar(String metodo, String ruta) throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest(metodo, ruta);
        cadena = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(peticion, response, cadena);
        return response;
    }

    private static void autenticar(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null,
                        AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }
}
