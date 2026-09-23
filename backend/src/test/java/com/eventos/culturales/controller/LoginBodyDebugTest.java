package com.eventos.culturales.controller;

import com.eventos.culturales.config.JwtSecretKeyProvider;
import com.eventos.culturales.config.LoginRateLimitFilter;
import com.eventos.culturales.config.SecurityConfig;
import com.eventos.culturales.entities.Usuario;
import com.eventos.culturales.repositories.UsuarioRepository;
import com.eventos.culturales.services.JwtService;
import com.eventos.culturales.services.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtService.class, JwtSecretKeyProvider.class})
// Regresión: el filtro de rate limiting lee el body para extraer el email,
// pero DEBE re-servirlo intacto al controller (un wrapper de un solo uso lo agotaba → 500).
class LoginFilterBodyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void login_conBody_llegaAlController() throws Exception {
        Usuario admin = Usuario.builder().id(1L).email("admin@test.com").roles(Set.of("ROLE_ADMIN")).enabled(true).nombre("Admin").apellidos("Sistema").build();
        Authentication auth = new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(refreshTokenService.createRefreshToken(any())).thenReturn(
                com.eventos.culturales.entities.RefreshToken.builder().token("rt-123").usuario(admin).build());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"x\"}"))
                .andExpect(status().isOk());
    }
}
