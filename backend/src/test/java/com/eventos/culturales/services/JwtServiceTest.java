package com.eventos.culturales.services;

import com.eventos.culturales.config.JwtSecretKeyProvider;
import com.eventos.culturales.entities.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

// Regresión: el algoritmo de firma DEBE coincidir con el del JwtDecoder
// (un HS256 firmado contra un decoder HS384 daba 401 en todo lo autenticado).
class JwtServiceTest {

    private static final String CLAVE_32_BYTES = "dGVzdC1zZWNyZXQta2V5LWZvci1sb2NhbC10ZXN0cy0xMjM0NTY3ODkwMTIz";

    @Test
    void tokenGenerado_loValidaElDecoder() {
        JwtSecretKeyProvider provider = new JwtSecretKeyProvider(CLAVE_32_BYTES);
        JwtService jwtService = new JwtService(provider);
        Usuario admin = Usuario.builder().id(1L).email("admin@test.com").roles(Set.of("ROLE_ADMIN")).enabled(true).build();

        String token = jwtService.generateToken(admin);

        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(provider.getSecretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        Jwt jwt = decoder.decode(token);

        assertEquals("admin@test.com", jwt.getSubject());
        assertEquals("[ROLE_ADMIN]", jwt.getClaimAsStringList("roles").toString());
    }
}
