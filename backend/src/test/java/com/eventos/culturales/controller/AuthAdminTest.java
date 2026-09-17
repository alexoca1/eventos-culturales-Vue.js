package com.eventos.culturales.controller;

import com.eventos.culturales.config.JwtSecretKeyProvider;
import com.eventos.culturales.config.SecurityConfig;
import com.eventos.culturales.entities.Usuario;
import com.eventos.culturales.repositories.UsuarioRepository;
import com.eventos.culturales.services.JwtService;
import com.eventos.culturales.services.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sin auto-registro público de admins: register crea USER; crear admin exige token ADMIN
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtService.class, JwtSecretKeyProvider.class})
class AuthAdminTest {

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

    // @EnableJpaAuditing exige JpaMappingContext, ausente en el slice @WebMvcTest
    @MockitoBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static RequestPostProcessor admin() {
        return jwt().authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private static RequestPostProcessor user() {
        return jwt().authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private static final String NUEVO = """
            {"email":"nuevo@test.com","password":"Secreto1",
             "nombre":"Nuevo","apellidos":"Admin","telefono":"600000000"}""";

    @Test
    void register_publico_creaUser_nuncaAdmin() throws Exception {
        when(usuarioRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Secreto1")).thenReturn("hash");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isCreated());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("ROLE_USER", captor.getValue().getRoles());
    }

    @Test
    void crearAdmin_anonimo_401() throws Exception {
        mockMvc.perform(post("/auth/usuarios-admin").contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearAdmin_user_403() throws Exception {
        mockMvc.perform(post("/auth/usuarios-admin").with(user()).contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearAdmin_admin_201_conRolAdmin() throws Exception {
        when(usuarioRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Secreto1")).thenReturn("hash");

        mockMvc.perform(post("/auth/usuarios-admin").with(admin()).contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isCreated());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("ROLE_ADMIN", captor.getValue().getRoles());
    }

    @Test
    void crearAdmin_emailDuplicado_400() throws Exception {
        when(usuarioRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.of(new Usuario()));

        mockMvc.perform(post("/auth/usuarios-admin").with(admin()).contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isBadRequest());
    }
}
