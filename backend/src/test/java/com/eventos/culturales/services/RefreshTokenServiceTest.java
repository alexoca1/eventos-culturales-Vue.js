package com.eventos.culturales.services;

import com.eventos.culturales.entities.RefreshToken;
import com.eventos.culturales.entities.Usuario;
import com.eventos.culturales.repositories.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private Usuario usuario() {
        return Usuario.builder().id(1L).email("admin@test.com").roles(Set.of("ROLE_ADMIN")).enabled(true).build();
    }

    private RefreshToken tokenValido(Usuario u) {
        return RefreshToken.builder().id(1L).token("tok-123").usuario(u)
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS)).revoked(false).build();
    }

    @Test
    void rotate_conTokenValido_revocaElAnteriorYDevuelveUnoNuevo() {
        Usuario u = usuario();
        RefreshToken anterior = tokenValido(u);
        when(refreshTokenRepository.findByToken("tok-123")).thenReturn(Optional.of(anterior));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken nuevo = refreshTokenService.rotateRefreshToken("tok-123");

        assertTrue(anterior.isRevoked());
        assertNotNull(nuevo.getToken());
        assertNotEquals(anterior.getToken(), nuevo.getToken());
        assertFalse(nuevo.isRevoked());
        verify(refreshTokenRepository).save(anterior);
    }

    @Test
    void rotate_conTokenRevocado_invalidaTodaLaSesion() {
        Usuario u = usuario();
        RefreshToken reutilizado = tokenValido(u);
        reutilizado.setRevoked(true);
        when(refreshTokenRepository.findByToken("tok-123")).thenReturn(Optional.of(reutilizado));

        assertThrows(BadCredentialsException.class, () -> refreshTokenService.rotateRefreshToken("tok-123"));
        verify(refreshTokenRepository).deleteByUsuario(u);
    }

    @Test
    void rotate_conTokenExpirado_loEliminaYFalla() {
        Usuario u = usuario();
        RefreshToken expirado = tokenValido(u);
        expirado.setExpiryDate(Instant.now().minus(1, ChronoUnit.HOURS));
        when(refreshTokenRepository.findByToken("tok-123")).thenReturn(Optional.of(expirado));

        assertThrows(BadCredentialsException.class, () -> refreshTokenService.rotateRefreshToken("tok-123"));
        verify(refreshTokenRepository).delete(expirado);
    }

    @Test
    void rotate_conTokenInexistenteONulo_falla() {
        when(refreshTokenRepository.findByToken("no-existe")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> refreshTokenService.rotateRefreshToken("no-existe"));
        assertThrows(BadCredentialsException.class, () -> refreshTokenService.rotateRefreshToken(null));
        assertThrows(BadCredentialsException.class, () -> refreshTokenService.rotateRefreshToken("  "));
    }

    @Test
    void revoke_marcaComoRevocado() {
        Usuario u = usuario();
        RefreshToken t = tokenValido(u);
        when(refreshTokenRepository.findByToken("tok-123")).thenReturn(Optional.of(t));

        refreshTokenService.revokeRefreshToken("tok-123");

        assertTrue(t.isRevoked());
        verify(refreshTokenRepository).save(t);
    }
}
