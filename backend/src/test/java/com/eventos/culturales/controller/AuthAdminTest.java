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

    // 006 US2: ascender ROLE_USER → ROLE_ORGANIZADOR (punto 4: con datos de organización)
    @Test
    void actualizarUsuario_ascenderAOrganizador_200() throws Exception {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioUser()));

        mockMvc.perform(put("/auth/usuarios/2").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"ROLE_ORGANIZADOR\"],\"enabled\":true,"
                                + "\"nombreOrganizacion\":\"Peña El Bombo\",\"encargadoNombre\":\"Ana\","
                                + "\"encargadoTelefono\":\"600111222\",\"encargadoEmail\":\"ana@bom.bo\"}"))
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

    // Punto 1: un usuario = un único rol → más de uno → 400
    @Test
    void actualizarUsuario_variosRoles_400() throws Exception {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioUser()));

        mockMvc.perform(put("/auth/usuarios/2").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"ROLE_USER\",\"ROLE_ADMIN\"],\"enabled\":true}"))
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
    // (punto 1: rol único — ya no se puede "añadir otro rol", solo conservar el propio)
    @Test
    void actualizarUsuario_propioSinRiesgo_200() throws Exception {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(adminPropio()));
        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminPropio()));

        mockMvc.perform(put("/auth/usuarios/1").with(adminComo("admin@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"ROLE_ADMIN\"],\"enabled\":true}"))
                .andExpect(status().isOk());

        verify(usuarioRepository).save(any(com.eventos.culturales.entities.Usuario.class));
    }

    // Punto 4: teléfono obligatorio en el registro
    @Test
    void register_sinTelefono_400() throws Exception {
        String sinTelefono = NUEVO.replace(",\"telefono\":\"600000000\"", "");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(sinTelefono))
                .andExpect(status().isBadRequest());
    }

    // Punto 4: ascender a ORGANIZADOR sin datos de organización → 400
    @Test
    void actualizarUsuario_organizadorSinDatos_400() throws Exception {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioUser()));

        mockMvc.perform(put("/auth/usuarios/2").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"ROLE_ORGANIZADOR\"],\"enabled\":true}"))
                .andExpect(status().isBadRequest());

        verify(usuarioRepository, never()).save(any(com.eventos.culturales.entities.Usuario.class));
    }

    // Punto 4: editar el propio perfil
    @Test
    void perfil_editar_ok() throws Exception {
        when(usuarioRepository.findByEmail("user")).thenReturn(Optional.of(usuarioUser()));

        mockMvc.perform(put("/auth/perfil").with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo\",\"telefono\":\"600111222\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo"))
                .andExpect(jsonPath("$.telefono").value("600111222"));

        ArgumentCaptor<com.eventos.culturales.entities.Usuario> captor =
                ArgumentCaptor.forClass(com.eventos.culturales.entities.Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("Nuevo", captor.getValue().getNombre());
        assertEquals("600111222", captor.getValue().getTelefono());
    }

    @Test
    void perfil_telefonoVacio_400() throws Exception {
        when(usuarioRepository.findByEmail("user")).thenReturn(Optional.of(usuarioUser()));

        mockMvc.perform(put("/auth/perfil").with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefono\":\"  \"}"))
                .andExpect(status().isBadRequest());

        verify(usuarioRepository, never()).save(any(com.eventos.culturales.entities.Usuario.class));
    }

    @Test
    void perfil_noOrg_ignoraDatosOrg() throws Exception {
        when(usuarioRepository.findByEmail("user")).thenReturn(Optional.of(usuarioUser()));

        mockMvc.perform(put("/auth/perfil").with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreOrganizacion\":\"Peña X\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<com.eventos.culturales.entities.Usuario> captor =
                ArgumentCaptor.forClass(com.eventos.culturales.entities.Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(null, captor.getValue().getNombreOrganizacion());
    }

    private com.eventos.culturales.entities.Usuario usuarioOrganizador() {
        com.eventos.culturales.entities.Usuario u = usuarioUser();
        u.setId(3L);
        u.setEmail("org@test.com");
        u.setRoles(java.util.Set.of("ROLE_ORGANIZADOR"));
        return u;
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor comoOrganizador() {
        return jwt().jwt(b -> b.subject("org@test.com")).authorities(
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ORGANIZADOR")));
    }

    @Test
    void perfil_organizador_incompleto_400() throws Exception {
        when(usuarioRepository.findByEmail("org@test.com")).thenReturn(Optional.of(usuarioOrganizador()));

        mockMvc.perform(put("/auth/perfil").with(comoOrganizador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Org\"}"))
                .andExpect(status().isBadRequest());

        verify(usuarioRepository, never()).save(any(com.eventos.culturales.entities.Usuario.class));
    }

    @Test
    void perfil_organizador_completo_200() throws Exception {
        when(usuarioRepository.findByEmail("org@test.com")).thenReturn(Optional.of(usuarioOrganizador()));

        mockMvc.perform(put("/auth/perfil").with(comoOrganizador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreOrganizacion\":\"Peña X\",\"encargadoNombre\":\"Ana\","
                                + "\"encargadoTelefono\":\"600111222\",\"encargadoEmail\":\"ana@x.es\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreOrganizacion").value("Peña X"));

        ArgumentCaptor<com.eventos.culturales.entities.Usuario> captor =
                ArgumentCaptor.forClass(com.eventos.culturales.entities.Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("ana@x.es", captor.getValue().getEncargadoEmail());
    }
}
