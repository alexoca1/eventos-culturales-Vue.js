// SPDX-License-Identifier: MIT

package com.eventos.culturales.services;

import com.eventos.culturales.entities.RefreshToken;
import com.eventos.culturales.entities.Usuario;
import com.eventos.culturales.repositories.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final long REFRESH_TOKEN_DURATION_DAYS = 7;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken createRefreshToken(Usuario usuario) {
        Objects.requireNonNull(usuario, "El usuario no puede ser nulo");

        RefreshToken refreshToken = RefreshToken.builder()
                .usuario(usuario)
                .token(UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString())
                .expiryDate(Instant.now().plus(REFRESH_TOKEN_DURATION_DAYS, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(String tokenStr) {
        if (tokenStr == null || tokenStr.isBlank()) {
            throw new BadCredentialsException("Refresh token no proporcionado");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new BadCredentialsException("Refresh token no válido"));

        Usuario usuario = refreshToken.getUsuario();

        // Detección de reutilización (Token Reuse Detection)
        if (refreshToken.isRevoked()) {
            // Un token revocado fue presentado de nuevo: posible compromiso de sesión.
            // Invalidar todos los tokens del usuario por seguridad.
            refreshTokenRepository.deleteByUsuario(usuario);
            throw new BadCredentialsException("Detectada reutilización de token revocado. Sesión invalidada.");
        }

        // Validación de expiración
        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadCredentialsException("Refresh token expirado");
        }

        // Marcar el token actual como revocado para detectar futuros intentos de reutilización
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Generar y persistir el nuevo token rotado
        return createRefreshToken(usuario);
    }

    @Transactional
    public void revokeRefreshToken(String tokenStr) {
        if (tokenStr != null && !tokenStr.isBlank()) {
            refreshTokenRepository.findByToken(tokenStr).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    @Transactional
    public void purgeExpiredTokens() {
        refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
    }

    /** 018 FR-001: supresión de cuenta — borra TODOS los refresh tokens del usuario. */
    @Transactional
    public void revokeAllFor(Usuario usuario) {
        refreshTokenRepository.deleteByUsuario(usuario);
    }
}
