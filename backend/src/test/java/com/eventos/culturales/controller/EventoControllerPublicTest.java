package com.eventos.culturales.controller;

import com.eventos.culturales.config.JwtSecretKeyProvider;
import com.eventos.culturales.config.SecurityConfig;
import com.eventos.culturales.entities.EstadoEvento;
import com.eventos.culturales.entities.Etiqueta;
import com.eventos.culturales.entities.Evento;
import com.eventos.culturales.repositories.UsuarioRepository;
import com.eventos.culturales.repositories.FavoritoRepository;
import com.eventos.culturales.services.EmailService;
import com.eventos.culturales.repositories.EventoRepository;
import com.eventos.culturales.services.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sin token: la consulta pública por fecha debe funcionar (cadena 1 de SecurityConfig)
@WebMvcTest(EventoController.class)
@Import({SecurityConfig.class, JwtService.class, JwtSecretKeyProvider.class})
class EventoControllerPublicTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventoRepository eventoRepository;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private FavoritoRepository favoritoRepository;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private com.eventos.culturales.repositories.EtiquetaRepository etiquetaRepository;

    // 015 US2: dependencia del constructor de EventoController (galería de fotos)
    @MockitoBean
    private com.eventos.culturales.repositories.FotoGaleriaRepository fotoGaleriaRepository;

    // @EnableJpaAuditing exige JpaMappingContext, ausente en el slice @WebMvcTest
    @MockitoBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private Evento evento(Long id, String fecha) {
        Evento e = new Evento();
        e.setId(id);
        e.setEstablecimiento("El mesoncito");
        e.setDireccion("C. Aduana, 3, Puertollano");
        e.setFecha(LocalDate.parse(fecha));
        e.setDescripcion("Fiesta Mexicana");
        return e;
    }

    private Evento eventoConEtiquetas(Long id, String fecha, String... nombres) {
        Evento e = evento(id, fecha);
        java.util.Set<Etiqueta> tags = new java.util.HashSet<>();
        long n = 1;
        for (String nombre : nombres) {
            tags.add(new Etiqueta(n++, nombre));
        }
        e.setEtiquetas(tags);
        return e;
    }

    @Test
    void getPorFecha_devuelveTodosLosDelDia() throws Exception {
        when(eventoRepository.findVigentesEnPorEstado(EstadoEvento.APROBADO, LocalDate.parse("2026-10-01")))
                .thenReturn(List.of(evento(1L, "2026-10-01"), evento(2L, "2026-10-01")));

        mockMvc.perform(get("/eventos").param("fecha", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].establecimiento").value("El mesoncito"));
    }

    @Test
    void getPorFechaSinEventos_devuelveListaVacia() throws Exception {
        when(eventoRepository.findVigentesEnPorEstado(EstadoEvento.APROBADO, LocalDate.parse("2026-10-02")))
                .thenReturn(List.of());

        mockMvc.perform(get("/eventos").param("fecha", "2026-10-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getSinFecha_devuelveTodos() throws Exception {
        when(eventoRepository.findByEstadoOrderByFechaAscIdAsc(eq(EstadoEvento.APROBADO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(evento(1L, "2026-10-01"))));

        mockMvc.perform(get("/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getPorIdInexistente_devuelve404() throws Exception {
        when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/eventos/999"))
                .andExpect(status().isNotFound());
    }

    // 017 US1/FR-001: creadoPor solo id/email/nombreOrganizacion.
    // Antes salía el Usuario entero: password (hash), teléfono, apellidos, encargado y roles.
    @Test
    void getPorId_creadoPorSoloExponeTresCampos() throws Exception {
        Evento e = evento(1L, "2026-10-01");
        e.setCreadoPor(com.eventos.culturales.entities.Usuario.builder()
                .id(7L).email("org@test.com").password("hash-bcrypt")
                .nombre("Ana").apellidos("García").telefono("600000000")
                .nombreOrganizacion("Asociación Cultural")
                .encargadoNombre("Luis").encargadoTelefono("600111222").encargadoEmail("luis@test.com")
                .roles(java.util.Set.of("ROLE_ORGANIZADOR")).enabled(true)
                .build());
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));

        mockMvc.perform(get("/eventos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creadoPor.id").value(7))
                .andExpect(jsonPath("$.creadoPor.email").value("org@test.com"))
                .andExpect(jsonPath("$.creadoPor.nombreOrganizacion").value("Asociación Cultural"))
                .andExpect(jsonPath("$.creadoPor.password").doesNotExist())
                .andExpect(jsonPath("$.creadoPor.telefono").doesNotExist())
                .andExpect(jsonPath("$.creadoPor.apellidos").doesNotExist())
                .andExpect(jsonPath("$.creadoPor.encargadoNombre").doesNotExist())
                .andExpect(jsonPath("$.creadoPor.encargadoTelefono").doesNotExist())
                .andExpect(jsonPath("$.creadoPor.encargadoEmail").doesNotExist())
                .andExpect(jsonPath("$.creadoPor.roles").doesNotExist())
                .andExpect(jsonPath("$.creadoPor.enabled").doesNotExist());
    }

    @Test
    void getConFechaInvalida_devuelve400() throws Exception {
        mockMvc.perform(get("/eventos").param("fecha", "no-es-fecha"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPorEtiquetas_devuelveLosQueTenganAlguna() throws Exception {
        when(eventoRepository.findDistinctByEstadoAndEtiquetasNombreInOrderByIdAsc(
                eq(EstadoEvento.APROBADO), eq(List.of("TEATRO", "MUSICA")), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(eventoConEtiquetas(1L, "2026-10-01", "TEATRO"))));

        mockMvc.perform(get("/eventos").param("etiquetas", "TEATRO,MUSICA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].etiquetas[0].nombre").value("TEATRO"));
    }

    @Test
    void getPorFechaYEtiquetas_devuelveSoloLosQueCumplenAmbas() throws Exception {
        when(eventoRepository.findVigentesEnPorEstadoYEtiquetas(
                EstadoEvento.APROBADO, LocalDate.parse("2026-10-10"), List.of("CINE")))
                .thenReturn(List.of(eventoConEtiquetas(2L, "2026-10-10", "CINE")));

        mockMvc.perform(get("/eventos").param("fecha", "2026-10-10").param("etiquetas", "CINE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getConEtiquetasVacias_seTrataComoAusente() throws Exception {
        when(eventoRepository.findByEstadoOrderByFechaAscIdAsc(eq(EstadoEvento.APROBADO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(evento(1L, "2026-10-01"))));

        mockMvc.perform(get("/eventos").param("etiquetas", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getConEtiquetaDesconocida_devuelveVacio() throws Exception {
        when(eventoRepository.findDistinctByEstadoAndEtiquetasNombreInOrderByIdAsc(
                eq(EstadoEvento.APROBADO), eq(List.of("ROCK")), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/eventos").param("etiquetas", "ROCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void getFuturos_devuelveSoloVigentesHoyODespues() throws Exception {
        when(eventoRepository.findFuturosPorEstado(
                eq(EstadoEvento.APROBADO), eq(LocalDate.now()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(eventoConEtiquetas(1L, "2026-10-01", "TEATRO"))));

        mockMvc.perform(get("/eventos").param("futuros", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getFuturosConEtiquetas_combinaAmbos() throws Exception {
        when(eventoRepository.findFuturosPorEstadoYEtiquetas(
                eq(EstadoEvento.APROBADO), eq(LocalDate.now()), eq(List.of("MUSICA")), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(eventoConEtiquetas(2L, "2026-10-10", "MUSICA"))));

        mockMvc.perform(get("/eventos").param("futuros", "true").param("etiquetas", "MUSICA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getFechaYFuturos_sonExcluyentes_400() throws Exception {
        mockMvc.perform(get("/eventos").param("fecha", "2026-10-01").param("futuros", "true"))
                .andExpect(status().isBadRequest());
    }

    // T144: paginación con 25 eventos (el slice mockea el repositorio: lo que se
    // verifica es que el controller reenvía ?page=, pagina a 10 y serializa Page<>)
    private List<Evento> veinticincoEventos() {
        java.util.List<Evento> lista = new java.util.ArrayList<>();
        for (long i = 1; i <= 25; i++) {
            lista.add(evento(i, "2026-10-" + String.format("%02d", (int) ((i % 28) + 1))));
        }
        return lista;
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return jwt().authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor organizador() {
        return jwt().jwt(b -> b.subject("org@test.com")).authorities(
                List.of(new SimpleGrantedAuthority("ROLE_ORGANIZADOR")));
    }

    @Test
    void getFuturos_pagina0_totalElementsYDiez() throws Exception {
        List<Evento> todos = veinticincoEventos();
        when(eventoRepository.findFuturosPorEstado(
                eq(EstadoEvento.APROBADO), eq(LocalDate.now()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(todos.subList(0, 10), PageRequest.of(0, 10), 25));

        mockMvc.perform(get("/eventos").param("futuros", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void getFuturos_pagina1_reenviaPageAlRepositorio() throws Exception {
        List<Evento> todos = veinticincoEventos();
        when(eventoRepository.findFuturosPorEstado(
                eq(EstadoEvento.APROBADO), eq(LocalDate.now()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(todos.subList(10, 20), PageRequest.of(1, 10), 25));

        mockMvc.perform(get("/eventos").param("futuros", "true").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.content[0].id").value(11));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventoRepository).findFuturosPorEstado(eq(EstadoEvento.APROBADO), eq(LocalDate.now()), captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
    }

    @Test
    void getPagina99_devuelveVacioSinError() throws Exception {
        when(eventoRepository.findByEstadoOrderByFechaAscIdAsc(eq(EstadoEvento.APROBADO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(99, 10), 25));

        mockMvc.perform(get("/eventos").param("page", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(25));
    }

    @Test
    void getMios_pagina0_paginaCorrectamente() throws Exception {
        com.eventos.culturales.entities.Usuario org = new com.eventos.culturales.entities.Usuario();
        org.setId(3L);
        org.setEmail("org@test.com");
        org.setRoles(java.util.Set.of("ROLE_ORGANIZADOR"));
        org.setEnabled(true);
        when(usuarioRepository.findByEmail("org@test.com")).thenReturn(Optional.of(org));
        List<Evento> todos = veinticincoEventos();
        when(eventoRepository.findByCreadoPorOrderByIdAsc(eq(org), any(Pageable.class)))
                .thenReturn(new PageImpl<>(todos.subList(0, 10), PageRequest.of(0, 10), 25));

        mockMvc.perform(get("/eventos/mios").with(organizador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.size").value(10));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventoRepository).findByCreadoPorOrderByIdAsc(eq(org), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
    }

    @Test
    void getPendientes_sigueArrayPlano() throws Exception {
        when(eventoRepository.findByEstadoInOrderByIdAsc(
                List.of(EstadoEvento.PENDIENTE_REVISION, EstadoEvento.PENDIENTE_ELIMINACION)))
                .thenReturn(List.of(evento(1L, "2026-10-01"), evento(2L, "2026-10-02")));

        mockMvc.perform(get("/eventos/pendientes").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.content").doesNotExist())
                .andExpect(jsonPath("$.totalElements").doesNotExist());
    }

    // ---- 016: búsqueda por texto (?q=) ----

    @Test
    void getPorTexto_parcialEnNombre_devuelveElEvento() throws Exception {
        Evento e = evento(1L, "2026-10-01");
        e.setNombre("Concierto de Jazz");
        when(eventoRepository.buscarPorTextoYEstado(
                eq("jazz"), eq(EstadoEvento.APROBADO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(e)));

        mockMvc.perform(get("/eventos").param("q", "jazz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].nombre").value("Concierto de Jazz"));
    }

    @Test
    void getPorTexto_parcialEnEstablecimiento_devuelveElEvento() throws Exception {
        Evento e = evento(2L, "2026-10-02");
        e.setNombre("Obra de teatro");
        e.setEstablecimiento("Teatro Municipal");
        when(eventoRepository.buscarPorTextoYEstado(
                eq("teatro"), eq(EstadoEvento.APROBADO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(e)));

        mockMvc.perform(get("/eventos").param("q", "teatro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].establecimiento").value("Teatro Municipal"));
    }

    @Test
    void getPorTextoVacio_ignoraElParametroYComportamientoActual() throws Exception {
        when(eventoRepository.findByEstadoOrderByFechaAscIdAsc(eq(EstadoEvento.APROBADO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(evento(1L, "2026-10-01"))));

        mockMvc.perform(get("/eventos").param("q", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(eventoRepository, never()).buscarPorTextoYEstado(any(), any(), any());
        verify(eventoRepository, never()).buscarPorTexto(any(), any());
    }

    @Test
    void getPorTextoUnCaracter_devuelve400() throws Exception {
        mockMvc.perform(get("/eventos").param("q", "a"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPorTextoConFuturos_devuelve400() throws Exception {
        mockMvc.perform(get("/eventos").param("q", "texto").param("futuros", "true"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPorTextoConFecha_devuelve400() throws Exception {
        mockMvc.perform(get("/eventos").param("q", "texto").param("fecha", "2026-12-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPorTextoConComodin_seEscapaYNoFalla() throws Exception {
        when(eventoRepository.buscarPorTextoYEstado(
                eq("\\%"), eq(EstadoEvento.APROBADO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/eventos").param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void getPorTextoComoAdmin_veTodosLosEstados() throws Exception {
        Evento pendiente = evento(9L, "2026-10-09");
        pendiente.setNombre("Pendiente de jazz");
        pendiente.setEstado(EstadoEvento.PENDIENTE_REVISION);
        when(eventoRepository.buscarPorTexto(eq("jazz"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pendiente)));

        mockMvc.perform(get("/eventos").param("q", "jazz").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].estado").value("PENDIENTE_REVISION"));

        verify(eventoRepository, never()).buscarPorTextoYEstado(any(), any(), any());
    }
}
