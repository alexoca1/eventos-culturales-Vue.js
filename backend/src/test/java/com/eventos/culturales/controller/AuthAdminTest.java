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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    // 018: dependencias nuevas de AuthController (supresión y portabilidad RGPD)
    @MockitoBean
    private com.eventos.culturales.repositories.EventoRepository eventoRepository;

    @MockitoBean
    private com.eventos.culturales.repositories.FavoritoRepository favoritoRepository;

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

    // 017 US3/FR-003: mínimo 8 caracteres
    @Test
    void register_passwordCorta_400() throws Exception {
        String corta = NUEVO.replace("\"password\":\"Secreto1\"", "\"password\":\"Ab1\"");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(corta))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("La contraseña debe tener al menos 8 caracteres"));

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    // 017 US3/FR-003: 8+ no basta, exige mayúscula + minúscula + número
    @Test
    void register_passwordSinMayusculas_400() throws Exception {
        String debil = NUEVO.replace("\"password\":\"Secreto1\"", "\"password\":\"solominusculas1\"");

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(debil))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password")
                        .value("La contraseña debe contener mayúsculas, minúsculas y números"));

        verify(usuarioRepository, never()).save(any(Usuario.class));
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

    // ---- 023: el email propio es editable (es el `sub` del JWT) ----

    @Test
    void perfil_cambiaEmail_200() throws Exception {
        when(usuarioRepository.findByEmail("user")).thenReturn(Optional.of(usuarioUser()));
        when(usuarioRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(put("/auth/perfil").with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nuevo@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nuevo@test.com"));

        ArgumentCaptor<com.eventos.culturales.entities.Usuario> captor =
                ArgumentCaptor.forClass(com.eventos.culturales.entities.Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("nuevo@test.com", captor.getValue().getEmail());
    }

    // T023/AC2: Usuario.email es UNIQUE. Sin el 409 previo, el save revienta con
    // DataIntegrityViolationException y el usuario ve un 500 sin explicación.
    @Test
    void perfil_emailYaEnUso_409() throws Exception {
        when(usuarioRepository.findByEmail("user")).thenReturn(Optional.of(usuarioUser()));
        when(usuarioRepository.findByEmail("tomado@test.com")).thenReturn(Optional.of(new Usuario()));

        mockMvc.perform(put("/auth/perfil").with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"tomado@test.com\"}"))
                .andExpect(status().isConflict());

        verify(usuarioRepository, never()).save(any(com.eventos.culturales.entities.Usuario.class));
    }

    @Test
    void perfil_emailInvalido_400() throws Exception {
        when(usuarioRepository.findByEmail("user")).thenReturn(Optional.of(usuarioUser()));

        mockMvc.perform(put("/auth/perfil").with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"esto-no-es-correo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("El correo debe ser válido"));

        verify(usuarioRepository, never()).save(any(com.eventos.culturales.entities.Usuario.class));
    }

    // T023/AC4: mandar el mismo email solo en otro caso no es un cambio. Si lo fuera,
    // findByEmail("USER@TEST.COM") no encontraría nada en una BD con collation
    // case-insensitive pero el save sí fallaría por UNIQUE.
    @Test
    void perfil_mismoEmailOtroCaso_sinCambiar_200() throws Exception {
        when(usuarioRepository.findByEmail("user")).thenReturn(Optional.of(usuarioUser()));

        mockMvc.perform(put("/auth/perfil").with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"USER@TEST.COM\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<com.eventos.culturales.entities.Usuario> captor =
                ArgumentCaptor.forClass(com.eventos.culturales.entities.Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("user@test.com", captor.getValue().getEmail());
    }

    // T023/AC3: la cuenta del README no puede perder su email, o las credenciales
    // documentadas dejarían de funcionar.
    @Test
    void perfil_cuentaDemo_noCambiaEmail_403() throws Exception {
        Usuario demo = usuarioUser();
        demo.setEmail(com.eventos.culturales.config.DemoAccountProtectionFilter.DEMO_EMAIL);
        when(usuarioRepository.findByEmail(com.eventos.culturales.config.DemoAccountProtectionFilter.DEMO_EMAIL))
                .thenReturn(Optional.of(demo));

        mockMvc.perform(put("/auth/perfil")
                        .with(jwt().jwt(b -> b.subject(com.eventos.culturales.config.DemoAccountProtectionFilter.DEMO_EMAIL))
                                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"otro@test.com\"}"))
                .andExpect(status().isForbidden());

        verify(usuarioRepository, never()).save(any(com.eventos.culturales.entities.Usuario.class));
    }

    // ---- 018 RGPD: supresión de cuenta y portabilidad de datos ----

    // T184 / FR-001 / AC1: el PUT no es borrado — la supresión es DELETE y anonimiza
    @Test
    void perfil_suprimir_anonimizaYBorraDatos_200() throws Exception {
        when(usuarioRepository.findByEmail("user")).thenReturn(Optional.of(usuarioUser()));

        mockMvc.perform(delete("/auth/perfil").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cuenta eliminada y datos anonimizados"));

        verify(refreshTokenService).revokeAllFor(any(Usuario.class));
        verify(favoritoRepository).deleteByUsuario(any(Usuario.class));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario anonimo = captor.getValue();

        assertTrue(anonimo.getEmail().startsWith("deleted_"));
        assertTrue(anonimo.getEmail().endsWith("@removed.local"));
        assertNotEquals("user@test.com", anonimo.getEmail());   // AC2: con el email antiguo ya no hay cuenta
        assertEquals("Usuario eliminado", anonimo.getNombre());
        assertEquals("", anonimo.getPassword());
        assertFalse(anonimo.getEnabled());
        assertNull(anonimo.getTelefono());
        assertNull(anonimo.getApellidos());
        assertNull(anonimo.getNombreOrganizacion());
        assertNull(anonimo.getEncargadoNombre());
        assertNull(anonimo.getEncargadoTelefono());
        assertNull(anonimo.getEncargadoEmail());
        // la fila NO se borra: los eventos que creó siguen teniendo a quién apuntar
        assertEquals(2L, anonimo.getId());
        assertEquals(Set.of("ROLE_USER"), anonimo.getRoles());
    }

    @Test
    void perfil_suprimir_sinToken_401() throws Exception {
        mockMvc.perform(delete("/auth/perfil")).andExpect(status().isUnauthorized());

        verify(favoritoRepository, never()).deleteByUsuario(any(Usuario.class));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    // T184 / FR-002 / AC3: extracto con perfil, roles, eventos creados y favoritos
    @Test
    void perfil_exportar_devuelveTodoSinPassword_200() throws Exception {
        com.eventos.culturales.entities.Usuario u = usuarioUser();
        u.setFechaRegistro(java.time.LocalDateTime.of(2026, 1, 15, 10, 0));
        when(usuarioRepository.findByEmail("user")).thenReturn(Optional.of(u));

        com.eventos.culturales.entities.Evento creado =
                new com.eventos.culturales.entities.Evento();
        creado.setId(11L);
        creado.setNombre("Concierto");
        when(eventoRepository.findByCreadoPor(any(Usuario.class))).thenReturn(List.of(creado));

        com.eventos.culturales.entities.Evento favorito =
                new com.eventos.culturales.entities.Evento();
        favorito.setId(22L);
        favorito.setNombre("Teatro");
        com.eventos.culturales.entities.Favorito f =
                new com.eventos.culturales.entities.Favorito();
        f.setEvento(favorito);
        when(favoritoRepository.findByUsuario(any(Usuario.class))).thenReturn(List.of(f));

        mockMvc.perform(get("/auth/perfil/exportar").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.nombre").value("User"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.fechaRegistro").value("2026-01-15T10:00:00"))
                .andExpect(jsonPath("$.eventosCreados[0].id").value(11))
                .andExpect(jsonPath("$.eventosCreados[0].nombre").value("Concierto"))
                .andExpect(jsonPath("$.favoritos[0].id").value(22))
                .andExpect(jsonPath("$.favoritos[0].nombre").value("Teatro"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void perfil_exportar_sinToken_401() throws Exception {
        mockMvc.perform(get("/auth/perfil/exportar")).andExpect(status().isUnauthorized());

        verify(eventoRepository, never()).findByCreadoPor(any(Usuario.class));
        verify(favoritoRepository, never()).findByUsuario(any(Usuario.class));
    }
}
