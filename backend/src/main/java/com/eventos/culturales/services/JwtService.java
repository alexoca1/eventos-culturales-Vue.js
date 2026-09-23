package com.eventos.culturales.services;

import com.eventos.culturales.config.JwtSecretKeyProvider;
import com.eventos.culturales.entities.Usuario;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtService {
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 15 * 60 * 1000; // 15 minutos
    private final JwtSecretKeyProvider jwtSecretKeyProvider;

    public String generateToken(Authentication authentication) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        return generateToken(usuario);
    }

    public String generateToken(Usuario usuario) {
        List<String> roles = usuario.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .toList();

        return Jwts.builder()
                .subject(usuario.getEmail())
                .issuer("eventos-culturales-api")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_MS))
                .claim("roles", roles)
                // HS256 explícito: coherente con el mínimo documentado de 32 bytes y con el JwtDecoder.
                // (signWith(key) a secas elegiría el algoritmo por longitud de clave y rompería la validación.)
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    public SecretKey getSecretKey() {
        return jwtSecretKeyProvider.getSecretKey();
    }
}
