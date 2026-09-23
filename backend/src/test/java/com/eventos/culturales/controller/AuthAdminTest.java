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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
        assertEquals(Set.of("ROLE_USER"), captor.getValue().getRoles());
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
        assertEquals(Set.of("ROLE_ADMIN"), captor.getValue().getRoles());
    }

    @Test
    void crearAdmin_emailDuplicado_400() throws Exception {
        when(usuarioRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.of(new Usuario()));

        mockMvc.perform(post("/auth/usuarios-admin").with(admin()).contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isBadRequest());
    }

    // 006 US1: el listado incluye enabled por usuario
    @Test
    void listarUsuarios_incluyeEnabled() throws Exception {
        com.eventos.culturales.entities.Usuario activo = new com.eventos.culturales.entities.Usuario();
        activo.setId(1L);
        activo.setEmail("a@test.com");
        activo.setEnabled(true);
        com.eventos.culturales.entities.Usuario inactivo = new com.eventos.culturales.entities.Usuario();
        inactivo.setId(2L);
        inactivo.setEmail("b@test.com");
        inactivo.setEnabled(false);
        when(usuarioRepository.findAll()).thenReturn(java.util.List.of(activo, inactivo));

        mockMvc.perform(get("/auth/usuarios").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[1].enabled").value(false));
    }

    private com.eventos.culturales.entities.Usuario usuarioUser() {
        com.eventos.culturales.entities.Usuario u = new com.eventos.culturales.entities.Usuario();
        u.setId(2L);
        u.setEmail("user@test.com");
        u.setPassword("hash");
        u.setNombre("User");
        u.setApellidos("Test");
        u.setRoles(java.util.Set.of("ROLE_USER"));
        u.setEnabled(true);
        return u;
    }

    // 006 US2: ascender ROLE_USER → ROLE_ORGANIZADOR
    @Test
    void actualizarUsuario_ascenderAOrganizador_200() throws Exception {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioUser()));

        mockMvc.perform(put("/auth/usuarios/2").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"ROLE_ORGANIZADOR\"],\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ORGANIZADOR"));

        ArgumentCaptor<com.eventos.culturales.entities.Usuario> captor =
                ArgumentCaptor.forClass(com.eventos.culturales.entities.Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(java.util.Set.of("ROLE_ORGANIZADOR"), captor.getValue().getRoles());
    }

    // 006 US2: desactivar una cuenta; un login posterior falla por isEnabled()
    @Test
    void actualizarUsuario_desactivar_200_yLoginPosteriorFalla() throws Exception {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioUser()));

        mockMvc.perform(put("/auth/usuarios/2").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk());

        ArgumentCaptor<com.eventos.culturales.entities.Usuario> captor =
                ArgumentCaptor.forClass(com.eventos.culturales.entities.Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(false, captor.getValue().getEnabled());

        // El mecanismo existente: DaoAuthenticationProvider rechaza usuarios deshabilitados
        com.eventos.culturales.entities.Usuario desactivado = captor.getValue();
        when(usuarioRepository.findByEmail("user@test.com")).thenReturn(Optional.of(desactivado));
        var provider = new org.springframework.security.authentication.dao.DaoAuthenticationProvider(
                new com.eventos.culturales.services.CustomUserDetailsService(usuarioRepository));
        provider.setPasswordEncoder(org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.class
                .getDeclaredConstructor().newInstance());
        assertThrows(org.springframework.security.authentication.DisabledException.class, () ->
                provider.authenticate(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "user@test.com", "cualquiera")));
    }

    // 006 US2: roles vacío o inválido → 400
    @Test
    void actualizarUsuario_rolesVacio_400() throws Exception {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioUser()));

        mockMvc.perform(put("/auth/usuarios/2").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[],\"enabled\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarUsuario_rolInvalido_400() throws Exception {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioUser()));

        mockMvc.perform(put("/auth/usuarios/2").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"ROLE_SUPER\"],\"enabled\":true}"))
                .andExpect(status().isBadRequest());
    }

    // 006 US2: no-admin → 403
    @Test
    void actualizarUsuario_user_403() throws Exception {
        mockMvc.perform(put("/auth/usuarios/2").with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"ROLE_ORGANIZADOR\"],\"enabled\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizarUsuario_inexistente_404() throws Exception {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/auth/usuarios/999").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"ROLE_ORGANIZADOR\"],\"enabled\":true}"))
                .andExpect(status().isNotFound());
    }

    private com.eventos.culturales.entities.Usuario adminPropio() {
        com.eventos.culturales.entities.Usuario u = new com.eventos.culturales.entities.Usuario();
        u.setId(1L);
        u.setEmail("admin@test.com");
        u.setPassword("hash");
        u.setNombre("Admin");
        u.setApellidos("Sistema");
        u.setRoles(java.util.Set.of("ROLE_ADMIN"));
        u.setEnabled(true);
        return u;
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor adminComo(String email) {
        return jwt().jwt(b -> b.subject(email)).authorities(
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    // 006 US3: desactivarse a sí mismo → 409 sin guardar
    @Test
    void actualizarUsuario_autodesactivarse_409() throws Exception {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(adminPropio()));
        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminPropio()));

        mockMvc.perform(put("/auth/usuarios/1").with(adminComo("admin@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isConflict());

        verify(usuarioRepository, never()).save(any(com.eventos.culturales.entities.Usuario.class));
    }

    // 006 US3: quitarse el propio ROLE_ADMIN → 409 sin guardar
    @Test
    void actualizarUsuario_quitarseRolAdmin_409() throws Exception {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(adminPropio()));
        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminPropio()));

        mockMvc.perform(put("/auth/usuarios/1").with(adminComo("admin@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"ROLE_ORGANIZADOR\"],\"enabled\":true}"))
                .andExpect(status().isConflict());

        verify(usuarioRepository, never()).save(any(com.eventos.culturales.entities.Usuario.class));
    }

    // 006 US3: editarse sin comprometer el acceso → se acepta
    @Test
    void actualizarUsuario_propioSinRiesgo_200() throws Exception {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(adminPropio()));
        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminPropio()));

        mockMvc.perform(put("/auth/usuarios/1").with(adminComo("admin@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"ROLE_ADMIN\",\"ROLE_ORGANIZADOR\"],\"enabled\":true}"))
                .andExpect(status().isOk());

        verify(usuarioRepository).save(any(com.eventos.culturales.entities.Usuario.class));
    }
}
