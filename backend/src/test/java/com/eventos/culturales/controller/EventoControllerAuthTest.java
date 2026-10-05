package com.eventos.culturales.controller;

import com.eventos.culturales.config.JwtSecretKeyProvider;
import com.eventos.culturales.config.SecurityConfig;
import com.eventos.culturales.entities.EstadoEvento;
import com.eventos.culturales.entities.Evento;
import com.eventos.culturales.entities.RedSocial;
import com.eventos.culturales.entities.Usuario;
import com.eventos.culturales.repositories.EventoRepository;
import com.eventos.culturales.repositories.FavoritoRepository;
import com.eventos.culturales.repositories.UsuarioRepository;
import com.eventos.culturales.services.EmailService;
import com.eventos.culturales.services.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Mutaciones: anónimo → 401, USER → 403, ADMIN → OK (cadena 2 + @PreAuthorize)
@WebMvcTest(EventoController.class)
@Import({SecurityConfig.class, JwtService.class, JwtSecretKeyProvider.class})
class EventoControllerAuthTest {

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

    private static RequestPostProcessor admin() {
        return jwt().authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private static RequestPostProcessor user() {
        return jwt().authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    // 007: organizador con subject propio (el create lo usa como creadoPor)
    private static RequestPostProcessor organizador() {
        return jwt().jwt(b -> b.subject("org@test.com"))
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ORGANIZADOR")));
    }

    // 019: cuenta demo (FR-001) — misma potencia que un admin, salvo los DELETE (FR-002)
    private static RequestPostProcessor demo() {
        return jwt().jwt(b -> b.subject("demo@eventos-culturales.es"))
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private static RequestPostProcessor otroOrganizador() {
        return jwt().jwt(b -> b.subject("otro@test.com"))
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ORGANIZADOR")));
    }

    private static Usuario organizadorEntity() {
        return Usuario.builder().id(2L).email("org@test.com")
                .roles(Set.of("ROLE_ORGANIZADOR")).enabled(true).nombre("Org").apellidos("Uno").build();
    }

    private static String eventoParaFecha(LocalDate fecha) {
        return """
                {"establecimiento":"Sala","direccion":"Calle 1",
                 "fecha":"FECHA","nombre":"Show","descripcion":"Show",
                 "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}"""
                .replace("FECHA", fecha.toString());
    }

    private static final String VALIDO = """
            {"establecimiento":"El mesoncito","direccion":"C. Aduana, 3, Puertollano",
             "fecha":"2026-10-01","nombre":"Fiesta Mexicana","descripcion":"Fiesta Mexicana",
             "cartelUrl":"",
             "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}""";

    private static final String SIN_ESTABLECIMIENTO = """
            {"establecimiento":"","direccion":"C. Aduana, 3, Puertollano",
             "fecha":"2026-10-01","nombre":"Fiesta Mexicana","descripcion":"Fiesta Mexicana"}""";

    private static final String MAPA_MALO = """
            {"establecimiento":"El mesoncito","direccion":"C. Aduana, 3, Puertollano",
             "fecha":"2026-10-01","nombre":"Fiesta Mexicana","descripcion":"Fiesta Mexicana",
             "mapaEmbed":"<iframe src=\\"https://evil.com/map\\"></iframe>"}""";

    private static final String MAPA_CON_HTML_EXTRA = """
            {"establecimiento":"El mesoncito","direccion":"C. Aduana, 3, Puertollano",
             "fecha":"2026-10-01","nombre":"Fiesta Mexicana","descripcion":"Fiesta Mexicana",
             "mapaEmbed":"<p>oferta</p><iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}""";

    private static final String MAPA_CON_SCRIPT = """
            {"establecimiento":"El mesoncito","direccion":"C. Aduana, 3, Puertollano",
             "fecha":"2026-10-01","nombre":"Fiesta Mexicana","descripcion":"Fiesta Mexicana",
             "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe><script>alert(1)</script>"}""";

    private static final String MAPA_CON_ATRIBUTO_MALO = """
            {"establecimiento":"El mesoncito","direccion":"C. Aduana, 3, Puertollano",
             "fecha":"2026-10-01","nombre":"Fiesta Mexicana","descripcion":"Fiesta Mexicana",
             "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\" onload=\\"alert(1)\\"></iframe>"}""";

    private static final String CON_HORARIO = """
            {"establecimiento":"El mesoncito","direccion":"C. Aduana, 3, Puertollano",
             "fecha":"2026-10-01","nombre":"Fiesta Mexicana","descripcion":"Fiesta Mexicana",
             "horaInicio":"20:00","horaFin":"22:30",
             "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}""";

    private static final String SOLO_INICIO = """
            {"establecimiento":"El mesoncito","direccion":"C. Aduana, 3, Puertollano",
             "fecha":"2026-10-01","nombre":"Fiesta Mexicana","descripcion":"Fiesta Mexicana",
             "horaInicio":"20:00",
             "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}""";

    private static final String SOLO_FIN = """
            {"establecimiento":"El mesoncito","direccion":"C. Aduana, 3, Puertollano",
             "fecha":"2026-10-01","nombre":"Fiesta Mexicana","descripcion":"Fiesta Mexicana",
             "horaFin":"22:30",
             "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}""";

    private static MockMultipartFile parteEvento(String json) {
        return new MockMultipartFile("evento", "", "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    // 017 FR-005: firmas reales; si los tests subieran bytes basura, el filtro de
    // magic bytes los rechazaría y todos estos casos darían 400 en vez del 201 esperado.
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, 1, 2, 3};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 4, 5, 6};

    private static MockMultipartFile imagenJpeg() {
        return new MockMultipartFile("file", "cartel.jpg", "image/jpeg", JPEG);
    }

    private Evento evento() {
        Evento e = new Evento();
        e.setId(1L);
        e.setEstablecimiento("El mesoncito");
        e.setDireccion("C. Aduana, 3, Puertollano");
        e.setFecha(LocalDate.parse("2026-10-01"));
        e.setNombre("Fiesta Mexicana");
        e.setDescripcion("Fiesta Mexicana");
        return e;
    }

    private Evento eventoDeOrg(EstadoEvento estado) {
        Evento e = evento();
        e.setEstado(estado);
        e.setCreadoPor(organizadorEntity());
        return e;
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder putEvento(
            Long id, String json, RequestPostProcessor auth) {
        return multipart("/eventos/" + id).file(parteEvento(json))
                .with(auth)
                .with(req -> { req.setMethod("PUT"); return req; });
    }

    @Test
    void post_anonimo_401() throws Exception {
        mockMvc.perform(multipart("/eventos").file(parteEvento(VALIDO)).file(imagenJpeg()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void post_user_403() throws Exception {
        mockMvc.perform(multipart("/eventos").file(parteEvento(VALIDO)).file(imagenJpeg()).with(user()))
                .andExpect(status().isForbidden());
    }

    @Test
    void post_admin_conImagen_201_yGuardaBytes() throws Exception {
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> {
            Evento e = i.getArgument(0);
            e.setId(1L);
            return e;
        });
        MockMultipartFile hd = new MockMultipartFile("fileHd", "cartel-hd.webp", "image/webp", WEBP);

        mockMvc.perform(multipart("/eventos").file(parteEvento(VALIDO)).file(imagenJpeg()).file(hd).with(admin()))
                .andExpect(status().isCreated());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertArrayEquals(JPEG, captor.getValue().getCartel());
        assertEquals("image/jpeg", captor.getValue().getCartelContentType());
        assertArrayEquals(WEBP, captor.getValue().getCartelHd());
        assertEquals("image/webp", captor.getValue().getCartelHdContentType());
    }

    @Test
    void post_admin_sinCartel_400() throws Exception {
        mockMvc.perform(multipart("/eventos").file(parteEvento(VALIDO)).with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_admin_invalido_400() throws Exception {
        mockMvc.perform(multipart("/eventos").file(parteEvento(SIN_ESTABLECIMIENTO)).with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_admin_mapaMalo_400() throws Exception {
        mockMvc.perform(multipart("/eventos").file(parteEvento(MAPA_MALO)).with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_admin_mapaConHtmlExtra_400() throws Exception {
        mockMvc.perform(multipart("/eventos").file(parteEvento(MAPA_CON_HTML_EXTRA)).with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_admin_mapaConScript_400() throws Exception {
        mockMvc.perform(multipart("/eventos").file(parteEvento(MAPA_CON_SCRIPT)).with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_admin_mapaConAtributoMalo_400() throws Exception {
        mockMvc.perform(multipart("/eventos").file(parteEvento(MAPA_CON_ATRIBUTO_MALO)).with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_admin_conHorario_seGuardaYSeRecuperaIgual() throws Exception {
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> {
            Evento e = i.getArgument(0);
            e.setId(1L);
            return e;
        });
        when(etiquetaRepository.findByNombre("OTROS"))
                .thenReturn(Optional.of(new com.eventos.culturales.entities.Etiqueta(7L, "OTROS")));

        mockMvc.perform(multipart("/eventos").file(parteEvento(CON_HORARIO)).file(imagenJpeg()).with(admin()))
                .andExpect(status().isCreated());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertEquals(java.time.LocalTime.of(20, 0), captor.getValue().getHoraInicio());
        assertEquals(java.time.LocalTime.of(22, 30), captor.getValue().getHoraFin());

        Evento guardado = evento();
        guardado.setHoraInicio(java.time.LocalTime.of(20, 0));
        guardado.setHoraFin(java.time.LocalTime.of(22, 30));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(guardado));

        mockMvc.perform(get("/eventos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horaInicio").value("20:00:00"))
                .andExpect(jsonPath("$.horaFin").value("22:30:00"));
    }

    @Test
    void post_admin_sinHorario_guardaNullsComoAntes() throws Exception {
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(etiquetaRepository.findByNombre("OTROS"))
                .thenReturn(Optional.of(new com.eventos.culturales.entities.Etiqueta(7L, "OTROS")));

        mockMvc.perform(multipart("/eventos").file(parteEvento(VALIDO)).file(imagenJpeg()).with(admin()))
                .andExpect(status().isCreated());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertEquals(null, captor.getValue().getHoraInicio());
        assertEquals(null, captor.getValue().getHoraFin());
    }

    @Test
    void post_admin_soloInicio_400() throws Exception {
        mockMvc.perform(multipart("/eventos").file(parteEvento(SOLO_INICIO)).with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_admin_soloFin_400() throws Exception {
        mockMvc.perform(multipart("/eventos").file(parteEvento(SOLO_FIN)).with(admin()))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------- 015 US1: nombre/descripcion
    @Test
    void post_admin_nombreYDescripcion_201_guardaAmbosYLosDevuelve() throws Exception {
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(etiquetaRepository.findByNombre("OTROS"))
                .thenReturn(Optional.of(new com.eventos.culturales.entities.Etiqueta(7L, "OTROS")));
        String json = """
                {"nombre":"Concierto de otoño","establecimiento":"Sala","direccion":"Calle 1",
                 "fecha":"2026-10-01","descripcion":"Tarde de música en vivo con la Big Band",
                 "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}""";

        mockMvc.perform(multipart("/eventos").file(parteEvento(json)).file(imagenJpeg()).with(admin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Concierto de otoño"))
                .andExpect(jsonPath("$.descripcion").value("Tarde de música en vivo con la Big Band"));

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertEquals("Concierto de otoño", captor.getValue().getNombre());
        assertEquals("Tarde de música en vivo con la Big Band", captor.getValue().getDescripcion());
    }

    @Test
    void post_admin_sinNombre_400() throws Exception {
        when(etiquetaRepository.findByNombre("OTROS"))
                .thenReturn(Optional.of(new com.eventos.culturales.entities.Etiqueta(7L, "OTROS")));
        String ausente = """
                {"establecimiento":"Sala","direccion":"Calle 1",
                 "fecha":"2026-10-01","descripcion":"Show",
                 "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}""";

        mockMvc.perform(multipart("/eventos").file(parteEvento(ausente)).file(imagenJpeg()).with(admin()))
                .andExpect(status().isBadRequest());

        // También en blanco: por eso es @NotBlank y no @NotNull (con @NotNull, "  " pasaría).
        String enBlanco = """
                {"nombre":"   ","establecimiento":"Sala","direccion":"Calle 1",
                 "fecha":"2026-10-01","descripcion":"Show"}""";

        mockMvc.perform(multipart("/eventos").file(parteEvento(enBlanco)).file(imagenJpeg()).with(admin()))
                .andExpect(status().isBadRequest());

        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void post_admin_sinDescripcion_201_esOpcional() throws Exception {
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(etiquetaRepository.findByNombre("OTROS"))
                .thenReturn(Optional.of(new com.eventos.culturales.entities.Etiqueta(7L, "OTROS")));
        String json = """
                {"nombre":"Charla corta","establecimiento":"Sala","direccion":"Calle 1",
                 "fecha":"2026-10-01",
                 "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}""";

        mockMvc.perform(multipart("/eventos").file(parteEvento(json)).file(imagenJpeg()).with(admin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Charla corta"));

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertNull(captor.getValue().getDescripcion(), "sin descripcion debe quedar null, no vacío");
    }

    @Test
    void put_admin_actualizaNombreYDescripcion() throws Exception {
        Evento existente = evento();
        existente.setNombre("Nombre viejo");
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(etiquetaRepository.findByNombre("OTROS"))
                .thenReturn(Optional.of(new com.eventos.culturales.entities.Etiqueta(7L, "OTROS")));
        String json = """
                {"nombre":"Nombre nuevo","establecimiento":"Sala","direccion":"Calle 1",
                 "fecha":"2026-10-01","descripcion":"Descripción nueva"}""";

        mockMvc.perform(multipart("/eventos/1").file(parteEvento(json))
                        .with(admin())
                        .with(req -> { req.setMethod("PUT"); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre nuevo"));

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertEquals("Nombre nuevo", captor.getValue().getNombre());
        assertEquals("Descripción nueva", captor.getValue().getDescripcion());
    }

    @Test
    void put_admin_sinNombre_400() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento()));
        String json = """
                {"establecimiento":"Sala","direccion":"Calle 1",
                 "fecha":"2026-10-01","descripcion":"Show"}""";

        mockMvc.perform(multipart("/eventos/1").file(parteEvento(json))
                        .with(admin())
                        .with(req -> { req.setMethod("PUT"); return req; }))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_fechaFin_variosDias() throws Exception {
        Evento e = evento();
        e.setFecha(java.time.LocalDate.parse("2026-11-11"));
        e.setFechaFin(java.time.LocalDate.parse("2026-11-17"));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));

        mockMvc.perform(get("/eventos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaFin").value("2026-11-17"));
    }

    @Test
    void get_fechaFin_mismoDia() throws Exception {
        Evento e = evento();
        e.setHoraInicio(java.time.LocalTime.of(20, 0));
        e.setHoraFin(java.time.LocalTime.of(22, 30));
        e.setFechaFin(java.time.LocalDate.parse("2026-10-01"));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));

        mockMvc.perform(get("/eventos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaFin").value("2026-10-01"));
    }

    @Test
    void get_fechaFin_sinHorario() throws Exception {
        Evento e = evento();
        e.setFechaFin(java.time.LocalDate.parse("2026-10-01"));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));

        mockMvc.perform(get("/eventos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaFin").value("2026-10-01"));
    }

    @Test
    void post_admin_fechaFinAnteriorAInicio_400() throws Exception {
        String json = """
                {"establecimiento":"Sala","direccion":"Calle 1",
                 "fecha":"2026-10-10","nombre":"Show","descripcion":"Show","fechaFin":"2026-10-09",
                 "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}""";

        mockMvc.perform(multipart("/eventos").file(parteEvento(json)).with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_admin_variosDias_seGuardaConFechaFin() throws Exception {
        String json = """
                {"establecimiento":"Circo","direccion":"Calle 1",
                 "fecha":"2026-11-11","nombre":"Circo","descripcion":"Circo","fechaFin":"2026-11-17",
                  "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}""";
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(etiquetaRepository.findByNombre("OTROS"))
                .thenReturn(Optional.of(new com.eventos.culturales.entities.Etiqueta(7L, "OTROS")));

        mockMvc.perform(multipart("/eventos").file(parteEvento(json)).file(imagenJpeg()).with(admin()))
                .andExpect(status().isCreated());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertEquals(java.time.LocalDate.parse("2026-11-17"), captor.getValue().getFechaFin());
    }

    @Test
    void post_admin_sinFechaFin_seNormalizaAFecha() throws Exception {
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(etiquetaRepository.findByNombre("OTROS"))
                .thenReturn(Optional.of(new com.eventos.culturales.entities.Etiqueta(7L, "OTROS")));

        mockMvc.perform(multipart("/eventos").file(parteEvento(VALIDO)).file(imagenJpeg()).with(admin()))
                .andExpect(status().isCreated());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertEquals(java.time.LocalDate.parse("2026-10-01"), captor.getValue().getFechaFin());
    }

    @Test
    void post_admin_variasEtiquetas_seGuardanYSeRecuperanIgual() throws Exception {
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(etiquetaRepository.findByNombre("MUSICA"))
                .thenReturn(Optional.of(new com.eventos.culturales.entities.Etiqueta(1L, "MUSICA")));
        when(etiquetaRepository.findByNombre("TEATRO"))
                .thenReturn(Optional.of(new com.eventos.culturales.entities.Etiqueta(2L, "TEATRO")));
        String json = """
                {"establecimiento":"Sala","direccion":"Calle 1",
                 "fecha":"2026-10-01","nombre":"Show","descripcion":"Show",
                 "etiquetas":["MUSICA","TEATRO"],
                 "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}""";

        mockMvc.perform(multipart("/eventos").file(parteEvento(json)).file(imagenJpeg()).with(admin()))
                .andExpect(status().isCreated());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository, atLeastOnce()).save(captor.capture());
        assertEquals(java.util.Set.of("MUSICA", "TEATRO"), captor.getValue().getEtiquetas().stream()
                .map(com.eventos.culturales.entities.Etiqueta::getNombre)
                .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void post_admin_sinEtiquetas_defectoOtros() throws Exception {
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(etiquetaRepository.findByNombre("OTROS"))
                .thenReturn(Optional.of(new com.eventos.culturales.entities.Etiqueta(7L, "OTROS")));

        mockMvc.perform(multipart("/eventos").file(parteEvento(VALIDO)).file(imagenJpeg()).with(admin()))
                .andExpect(status().isCreated());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertEquals(java.util.Set.of("OTROS"), captor.getValue().getEtiquetas().stream()
                .map(com.eventos.culturales.entities.Etiqueta::getNombre)
                .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void post_admin_etiquetaDesconocida_400() throws Exception {
        when(etiquetaRepository.findByNombre("ROCK")).thenReturn(Optional.empty());
        String json = """
                {"establecimiento":"Sala","direccion":"Calle 1",
                 "fecha":"2026-10-01","nombre":"Show","descripcion":"Show",
                 "etiquetas":["ROCK"],
                 "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}""";

        mockMvc.perform(multipart("/eventos").file(parteEvento(json)).with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_admin_tipoNoImagen_400() throws Exception {
        MockMultipartFile txt = new MockMultipartFile("file", "notas.txt", "text/plain", "hola".getBytes());

        mockMvc.perform(multipart("/eventos").file(parteEvento(VALIDO)).file(txt).with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_admin_gif_400() throws Exception {
        MockMultipartFile gif = new MockMultipartFile("file", "anim.gif", "image/gif", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/eventos").file(parteEvento(VALIDO)).file(gif).with(admin()))
                .andExpect(status().isBadRequest());
    }

    // 017 US2/FR-002: solo http(s) — javascript: no es un esquema admitido.
    @Test
    void post_admin_urlEventoJavascript_400() throws Exception {
        String json = """
                {"establecimiento":"Sala","direccion":"Calle 1",
                 "fecha":"2026-10-01","nombre":"Show","descripcion":"Show",
                 "urlEvento":"javascript:alert(1)"}""";

        mockMvc.perform(multipart("/eventos").file(parteEvento(json)).file(imagenJpeg()).with(admin()))
                .andExpect(status().isBadRequest());

        verify(eventoRepository, never()).save(any(Evento.class));
    }

    // 017 US4/FR-005: Content-Type de mentira; la firma real del fichero no corresponde.
    @Test
    void post_admin_imagenConFalsaFirma_400() throws Exception {
        MockMultipartFile exe = new MockMultipartFile("file", "cartel.jpg", "image/jpeg",
                "MZ\u0090\u0000\u0003".getBytes(StandardCharsets.ISO_8859_1));

        mockMvc.perform(multipart("/eventos").file(parteEvento(VALIDO)).file(exe).with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El archivo no es una imagen válida"));

        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void put_anonimo_401() throws Exception {
        mockMvc.perform(multipart("/eventos/1").file(parteEvento(VALIDO))
                        .with(req -> { req.setMethod("PUT"); return req; }))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void put_user_403() throws Exception {
        mockMvc.perform(multipart("/eventos/1").file(parteEvento(VALIDO))
                        .with(user())
                        .with(req -> { req.setMethod("PUT"); return req; }))
                .andExpect(status().isForbidden());
    }

    @Test
    void put_admin_existente_200() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento()));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(multipart("/eventos/1").file(parteEvento(VALIDO)).file(imagenJpeg())
                        .with(admin())
                        .with(req -> { req.setMethod("PUT"); return req; }))
                .andExpect(status().isOk());
    }

    @Test
    void put_admin_inexistente_404() throws Exception {
        when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(multipart("/eventos/999").file(parteEvento(VALIDO))
                        .with(admin())
                        .with(req -> { req.setMethod("PUT"); return req; }))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_anonimo_401() throws Exception {
        mockMvc.perform(delete("/eventos/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_user_403() throws Exception {
        mockMvc.perform(delete("/eventos/1").with(user()))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_admin_existente_204() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento()));

        mockMvc.perform(delete("/eventos/1").with(admin()))
                .andExpect(status().isNoContent());
    }

    // 008 US3: borrar directo con favoritos no falla y los limpia antes
    @Test
    void delete_admin_conFavoritos_limpiaFavoritos() throws Exception {
        Evento e = evento();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));

        mockMvc.perform(delete("/eventos/1").with(admin()))
                .andExpect(status().isNoContent());

        org.mockito.InOrder orden = org.mockito.Mockito.inOrder(favoritoRepository, eventoRepository);
        orden.verify(favoritoRepository).deleteByEvento(e);
        orden.verify(eventoRepository).deleteById(1L);
    }

    // 008 US3: aprobar una eliminación pendiente también limpia sus favoritos
    @Test
    void aprobar_pendienteEliminacion_limpiaFavoritos() throws Exception {
        Evento e = eventoDeOrg(EstadoEvento.PENDIENTE_ELIMINACION);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));

        mockMvc.perform(post("/eventos/1/aprobar").with(admin()))
                .andExpect(status().isNoContent());

        org.mockito.InOrder orden = org.mockito.Mockito.inOrder(favoritoRepository, eventoRepository);
        orden.verify(favoritoRepository).deleteByEvento(e);
        orden.verify(eventoRepository).delete(e);
    }

    @Test
    void delete_admin_inexistente_404() throws Exception {
        when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/eventos/999").with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBulk_anonimo_401() throws Exception {
        mockMvc.perform(delete("/eventos").param("ids", "1,2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteBulk_admin_204() throws Exception {
        mockMvc.perform(delete("/eventos").with(admin()).param("ids", "1,2"))
                .andExpect(status().isNoContent());
    }

    // 019 FR-002: el filtro está registrado en la cadena 2 y corta antes del controller
    @Test
    void delete_cuentaDemo_403() throws Exception {
        mockMvc.perform(delete("/eventos/1").with(demo()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error",
                        org.hamcrest.Matchers.containsString("modo demostración")));

        verify(eventoRepository, never()).findById(anyLong());
    }

    // 019 US1 AC2: a la cuenta demo solo se le cortan los DELETE, el resto funciona
    @Test
    void moderar_cuentaDemo_200() throws Exception {
        Evento e = eventoDeOrg(EstadoEvento.PENDIENTE_REVISION);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));

        mockMvc.perform(post("/eventos/1/aprobar").with(demo()))
                .andExpect(status().isOk());

        assertEquals(EstadoEvento.APROBADO, e.getEstado());
    }

    @Test
    void cartel_conImagen_200() throws Exception {
        Evento e = evento();
        e.setCartel(new byte[]{1, 2, 3});
        e.setCartelContentType("image/jpeg");
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));

        mockMvc.perform(get("/eventos/1/cartel"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void cartel_soloUrl_redirige() throws Exception {
        Evento e = evento();
        e.setCartelUrl("https://example.com/cartel.jpg");
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));

        mockMvc.perform(get("/eventos/1/cartel"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/cartel.jpg"));
    }

    @Test
    void cartel_inexistente_404() throws Exception {
        when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/eventos/999/cartel"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cartelHd_conImagen_200() throws Exception {
        Evento e = evento();
        e.setCartelHd(new byte[]{7, 8, 9});
        e.setCartelHdContentType("image/webp");
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));

        mockMvc.perform(get("/eventos/1/cartel-hd"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andExpect(content().bytes(new byte[]{7, 8, 9}));
    }

    @Test
    void cartelHd_inexistente_404() throws Exception {
        when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/eventos/999/cartel-hd"))
                .andExpect(status().isNotFound());
    }

    // 007 US1: organizador a +72h queda PENDIENTE_REVISION con creadoPor propio
    @Test
    void post_organizador_mas48h_pendienteRevision() throws Exception {
        when(usuarioRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizadorEntity()));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(etiquetaRepository.findByNombre("OTROS"))
                .thenReturn(Optional.of(new com.eventos.culturales.entities.Etiqueta(7L, "OTROS")));

        mockMvc.perform(multipart("/eventos")
                        .file(parteEvento(eventoParaFecha(LocalDate.now().plusDays(3))))
                        .file(imagenJpeg())
                        .with(organizador()))
                .andExpect(status().isCreated());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertEquals(EstadoEvento.PENDIENTE_REVISION, captor.getValue().getEstado());
        assertEquals("org@test.com", captor.getValue().getCreadoPor().getEmail());
    }

    // 007 US1: organizador a menos de 48h (mañana a las 00:00) es rechazado antes de guardar
    @Test
    void post_organizador_menos48h_400() throws Exception {
        when(usuarioRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizadorEntity()));

        mockMvc.perform(multipart("/eventos")
                        .file(parteEvento(eventoParaFecha(LocalDate.now().plusDays(1))))
                        .with(organizador()))
                .andExpect(status().isBadRequest());
    }

    // 007 US1: admin crea directo APROBADO sin regla de 48h (fecha de mañana mismo)
    @Test
    void post_admin_sinRegla48h_aprobadoDirecto() throws Exception {
        when(usuarioRepository.findByEmail("user")).thenReturn(Optional.empty());
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        when(etiquetaRepository.findByNombre("OTROS"))
                .thenReturn(Optional.of(new com.eventos.culturales.entities.Etiqueta(7L, "OTROS")));

        mockMvc.perform(multipart("/eventos")
                        .file(parteEvento(eventoParaFecha(LocalDate.now().plusDays(1))))
                        .file(imagenJpeg())
                        .with(admin()))
                .andExpect(status().isCreated());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertEquals(EstadoEvento.APROBADO, captor.getValue().getEstado());
    }

    // 007 US5: sin auth solo APROBADO; con admin, los 4 estados (+ filtros)
    @Test
    void get_sinAuth_soloAprobados() throws Exception {
        Evento aprobado = evento();
        aprobado.setEstado(EstadoEvento.APROBADO);
        when(eventoRepository.findByEstadoOrderByFechaAscIdAsc(eq(EstadoEvento.APROBADO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(aprobado)));

        mockMvc.perform(get("/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].estado").value("APROBADO"));
    }

    @Test
    void get_admin_futuros_veTodosLosEstados() throws Exception {
        Evento e = evento();
        when(eventoRepository.findFuturos(eq(java.time.LocalDate.now()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(e)));

        mockMvc.perform(get("/eventos").param("futuros", "true").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void get_admin_veLosCuatroEstados() throws Exception {
        Evento e = evento();
        when(eventoRepository.findAllByOrderByFechaAscIdAsc(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(e)));

        mockMvc.perform(get("/eventos").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void get_sinAuth_conFecha_soloAprobadosVigentes() throws Exception {
        Evento aprobado = evento();
        aprobado.setEstado(EstadoEvento.APROBADO);
        when(eventoRepository.findVigentesEnPorEstado(EstadoEvento.APROBADO, LocalDate.parse("2026-10-01")))
                .thenReturn(List.of(aprobado));

        mockMvc.perform(get("/eventos").param("fecha", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].estado").value("APROBADO"));
    }

    @Test
    void get_sinAuth_conEtiquetas_soloAprobados() throws Exception {
        Evento aprobado = evento();
        aprobado.setEtiquetas(new java.util.HashSet<>(java.util.List.of(
                new com.eventos.culturales.entities.Etiqueta(2L, "TEATRO"))));
        aprobado.setEstado(EstadoEvento.APROBADO);
        when(eventoRepository.findDistinctByEstadoAndEtiquetasNombreInOrderByIdAsc(
                eq(EstadoEvento.APROBADO), eq(java.util.List.of("TEATRO", "MUSICA")), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(aprobado)));

        mockMvc.perform(get("/eventos").param("etiquetas", "TEATRO,MUSICA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    private Evento pendiente(EstadoEvento estado) {
        Evento e = evento();
        e.setEstado(estado);
        return e;
    }

    // 007 US2: cola de moderación solo para admin
    @Test
    void pendientes_admin_200_conAmbosPendientes() throws Exception {
        when(eventoRepository.findByEstadoInOrderByIdAsc(
                List.of(EstadoEvento.PENDIENTE_REVISION, EstadoEvento.PENDIENTE_ELIMINACION)))
                .thenReturn(List.of(pendiente(EstadoEvento.PENDIENTE_REVISION),
                        pendiente(EstadoEvento.PENDIENTE_ELIMINACION)));

        mockMvc.perform(get("/eventos/pendientes").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void pendientes_organizador_403() throws Exception {
        mockMvc.perform(get("/eventos/pendientes").with(organizador()))
                .andExpect(status().isForbidden());
    }

    @Test
    void aprobar_revision_200_pasaAAprobado() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(pendiente(EstadoEvento.PENDIENTE_REVISION)));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/eventos/1/aprobar").with(admin()))
                .andExpect(status().isOk());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertEquals(EstadoEvento.APROBADO, captor.getValue().getEstado());
    }

    @Test
    void aprobar_organizador_403() throws Exception {
        mockMvc.perform(post("/eventos/1/aprobar").with(organizador()))
                .andExpect(status().isForbidden());
    }

    @Test
    void rechazar_conMotivo_200_guardaMotivo() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(pendiente(EstadoEvento.PENDIENTE_REVISION)));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/eventos/1/rechazar").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Falta información de contacto\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertEquals(EstadoEvento.RECHAZADO, captor.getValue().getEstado());
        assertEquals("Falta información de contacto", captor.getValue().getMotivoRechazo());
    }

    @Test
    void rechazar_organizador_403() throws Exception {
        mockMvc.perform(post("/eventos/1/rechazar").with(organizador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    // 007 US3: dueño edita APROBADO → vuelve a PENDIENTE_REVISION
    @Test
    void put_dueno_editaAprobado_vuelveARevision() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDeOrg(EstadoEvento.APROBADO)));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(putEvento(1L, VALIDO, organizador()))
                .andExpect(status().isOk());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertEquals(EstadoEvento.PENDIENTE_REVISION, captor.getValue().getEstado());
    }

    // 007 US3: dueño edita RECHAZADO → reenvío a PENDIENTE_REVISION
    @Test
    void put_dueno_editaRechazado_vuelveARevision() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDeOrg(EstadoEvento.RECHAZADO)));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(putEvento(1L, VALIDO, organizador()))
                .andExpect(status().isOk());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertEquals(EstadoEvento.PENDIENTE_REVISION, captor.getValue().getEstado());
    }

    // 007 US3: dueño edita PENDIENTE_ELIMINACION → 409
    @Test
    void put_dueno_editaPendienteEliminacion_409() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDeOrg(EstadoEvento.PENDIENTE_ELIMINACION)));

        mockMvc.perform(putEvento(1L, VALIDO, organizador()))
                .andExpect(status().isConflict());
    }

    // 007 US3: organizador NO dueño → 403
    @Test
    void put_noDueno_403() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDeOrg(EstadoEvento.APROBADO)));

        mockMvc.perform(putEvento(1L, VALIDO, otroOrganizador()))
                .andExpect(status().isForbidden());
    }

    // 007 US4: dueño pide eliminar APROBADO → PENDIENTE_ELIMINACION (no se borra)
    @Test
    void delete_dueno_pideEliminar_pasaAPendienteEliminacion() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDeOrg(EstadoEvento.APROBADO)));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(delete("/eventos/1").with(organizador()))
                .andExpect(status().isOk());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertEquals(EstadoEvento.PENDIENTE_ELIMINACION, captor.getValue().getEstado());
        verify(eventoRepository, never()).deleteById(anyLong());
        verify(eventoRepository, never()).delete(any(Evento.class));
    }

    // 007 US4: admin aprueba la solicitud → borrado definitivo
    @Test
    void aprobar_pendienteEliminacion_borraDefinitivo() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDeOrg(EstadoEvento.PENDIENTE_ELIMINACION)));

        mockMvc.perform(post("/eventos/1/aprobar").with(admin()))
                .andExpect(status().isNoContent());

        verify(eventoRepository).delete(any(Evento.class));
    }

    // 007 US4: admin rechaza la solicitud → vuelve a APROBADO
    @Test
    void rechazar_pendienteEliminacion_vuelveAAprobado() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDeOrg(EstadoEvento.PENDIENTE_ELIMINACION)));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/eventos/1/rechazar").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertEquals(EstadoEvento.APROBADO, captor.getValue().getEstado());
    }

    // 007 US4: organizador NO dueño no puede ni pedir eliminar
    @Test
    void delete_noDueno_403() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDeOrg(EstadoEvento.APROBADO)));

        mockMvc.perform(delete("/eventos/1").with(otroOrganizador()))
                .andExpect(status().isForbidden());
    }

    // 007 US3: admin edita cualquiera → APROBADO directo, como siempre
    @Test
    void put_admin_editaCualquiera_aprobadoDirecto() throws Exception {        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDeOrg(EstadoEvento.PENDIENTE_REVISION)));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(putEvento(1L, VALIDO, admin()))
                .andExpect(status().isOk());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertEquals(EstadoEvento.APROBADO, captor.getValue().getEstado());
    }

    // 007 US6: aprobar dispara email al dueño con los datos correctos
    @Test
    void aprobar_disparaEmail_alDueno() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDeOrg(EstadoEvento.PENDIENTE_REVISION)));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/eventos/1/aprobar").with(admin()))
                .andExpect(status().isOk());

        verify(emailService).enviar(
                org.mockito.ArgumentMatchers.eq("org@test.com"),
                org.mockito.ArgumentMatchers.eq("Tu evento ha sido APROBADO"),
                org.mockito.ArgumentMatchers.contains("Fiesta Mexicana"));
    }

    // 007 US6: rechazar dispara email con el motivo
    @Test
    void rechazar_disparaEmail_conMotivo() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDeOrg(EstadoEvento.PENDIENTE_REVISION)));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/eventos/1/rechazar").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Sin cartel\"}"))
                .andExpect(status().isOk());

        verify(emailService).enviar(
                org.mockito.ArgumentMatchers.eq("org@test.com"),
                org.mockito.ArgumentMatchers.eq("Tu evento ha sido RECHAZADO"),
                org.mockito.ArgumentMatchers.contains("Sin cartel"));
    }

    // 007 US6 (FR-013 escenario 3): si el email falla, el estado igual queda confirmado
    @Test
    void rechazar_falloEmail_noRevierteEstado() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDeOrg(EstadoEvento.PENDIENTE_REVISION)));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("Mailtrap caído")).when(emailService)
                .enviar(any(), any(), any());

        mockMvc.perform(post("/eventos/1/rechazar").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertEquals(EstadoEvento.RECHAZADO, captor.getValue().getEstado());
    }

    // 007 US6/T099: /mios solo para el organizador dueño
    @Test
    void mios_organizador_soloSuyos() throws Exception {
        when(usuarioRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizadorEntity()));
        when(eventoRepository.findByCreadoPorOrderByIdAsc(eq(organizadorEntity()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(eventoDeOrg(EstadoEvento.PENDIENTE_REVISION))));

        mockMvc.perform(get("/eventos/mios").with(organizador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void mios_admin_403() throws Exception {
        mockMvc.perform(get("/eventos/mios").with(admin()))
                .andExpect(status().isForbidden());
    }

    // T100: /mios y /pendientes ya no están en la cadena pública → anónimo 401
    @Test
    void pendientes_anonimo_401() throws Exception {
        mockMvc.perform(get("/eventos/pendientes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mios_anonimo_401() throws Exception {
        mockMvc.perform(get("/eventos/mios"))
                .andExpect(status().isUnauthorized());
    }

    private com.eventos.culturales.entities.Usuario usuarioComun() {
        return com.eventos.culturales.entities.Usuario.builder().id(3L).email("user")
                .roles(Set.of("ROLE_USER")).enabled(true).nombre("User").apellidos("Comun").build();
    }

    private Evento eventoAprobado() {
        Evento e = evento();
        e.setEstado(EstadoEvento.APROBADO);
        return e;
    }

    // 008 US1: marcar favorito sobre APROBADO lo crea
    @Test
    void post_favorito_aprobado_201() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoAprobado()));
        when(usuarioRepository.findByEmail("user")).thenReturn(Optional.of(usuarioComun()));
        when(favoritoRepository.findByUsuarioAndEvento(any(com.eventos.culturales.entities.Usuario.class), any(Evento.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/eventos/1/favorito").with(user()))
                .andExpect(status().isCreated());

        ArgumentCaptor<com.eventos.culturales.entities.Favorito> captor =
                ArgumentCaptor.forClass(com.eventos.culturales.entities.Favorito.class);
        verify(favoritoRepository).save(captor.capture());
        assertEquals("user", captor.getValue().getUsuario().getEmail());
        assertEquals(1L, captor.getValue().getEvento().getId());
    }

    // 008 US1: repetirlo es idempotente (no falla, no duplica)
    @Test
    void post_favorito_repetido_200_sinDuplicar() throws Exception {
        com.eventos.culturales.entities.Favorito existente = new com.eventos.culturales.entities.Favorito();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoAprobado()));
        when(usuarioRepository.findByEmail("user")).thenReturn(Optional.of(usuarioComun()));
        when(favoritoRepository.findByUsuarioAndEvento(any(com.eventos.culturales.entities.Usuario.class), any(Evento.class)))
                .thenReturn(Optional.of(existente));

        mockMvc.perform(post("/eventos/1/favorito").with(user()))
                .andExpect(status().isOk());

        verify(favoritoRepository, never()).save(any(com.eventos.culturales.entities.Favorito.class));
    }

    // 008 US1: sobre evento no APROBADO → 404 (no se expone su existencia)
    @Test
    void post_favorito_noAprobado_404() throws Exception {
        Evento pendiente = evento();
        pendiente.setEstado(EstadoEvento.PENDIENTE_REVISION);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(pendiente));

        mockMvc.perform(post("/eventos/1/favorito").with(user()))
                .andExpect(status().isNotFound());
    }

    // 008 US1: sin autenticar → 401
    @Test
    void post_favorito_anonimo_401() throws Exception {
        mockMvc.perform(post("/eventos/1/favorito"))
                .andExpect(status().isUnauthorized());
    }

    // 008 US1: desmarcar elimina si existía
    @Test
    void delete_favorito_existente_204() throws Exception {
        com.eventos.culturales.entities.Favorito existente = new com.eventos.culturales.entities.Favorito();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoAprobado()));
        when(usuarioRepository.findByEmail("user")).thenReturn(Optional.of(usuarioComun()));
        when(favoritoRepository.findByUsuarioAndEvento(any(com.eventos.culturales.entities.Usuario.class), any(Evento.class)))
                .thenReturn(Optional.of(existente));

        mockMvc.perform(delete("/eventos/1/favorito").with(user()))
                .andExpect(status().isNoContent());

        verify(favoritoRepository).delete(existente);
    }

    // 008 US1: desmarcar lo inexistente no falla
    @Test
    void delete_favorito_inexistente_204() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoAprobado()));
        when(usuarioRepository.findByEmail("user")).thenReturn(Optional.of(usuarioComun()));
        when(favoritoRepository.findByUsuarioAndEvento(any(com.eventos.culturales.entities.Usuario.class), any(Evento.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(delete("/eventos/1/favorito").with(user()))
                .andExpect(status().isNoContent());
    }

    private com.eventos.culturales.entities.Favorito favoritoDe(
            com.eventos.culturales.entities.Usuario u, Evento e) {
        com.eventos.culturales.entities.Favorito f = new com.eventos.culturales.entities.Favorito();
        f.setUsuario(u);
        f.setEvento(e);
        return f;
    }

    // 008 US2: mis favoritos = solo los míos que siguen APROBADO
    @Test
    void get_favoritos_soloMiosYAprobados() throws Exception {
        Evento aprobado = eventoAprobado();
        when(usuarioRepository.findByEmail("user")).thenReturn(Optional.of(usuarioComun()));
        when(favoritoRepository.findByUsuarioAndEventoEstado(
                usuarioComun(), EstadoEvento.APROBADO))
                .thenReturn(List.of(favoritoDe(usuarioComun(), aprobado)));

        mockMvc.perform(get("/eventos/favoritos").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].establecimiento").value("El mesoncito"));
    }

    // 008 US2: el que dejó de estar APROBADO ya no aparece (aunque siga en BD)
    @Test
    void get_favoritos_ocultaNoAprobados() throws Exception {
        when(usuarioRepository.findByEmail("user")).thenReturn(Optional.of(usuarioComun()));
        when(favoritoRepository.findByUsuarioAndEventoEstado(
                usuarioComun(), EstadoEvento.APROBADO))
                .thenReturn(List.of());

        mockMvc.perform(get("/eventos/favoritos").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void get_favoritos_anonimo_401() throws Exception {
        mockMvc.perform(get("/eventos/favoritos"))
                .andExpect(status().isUnauthorized());
    }

    // 010 US1: detalle de evento no público (mismo criterio que el listado)
    @Test
    void getId_aprobado_visibleSinAuth() throws Exception {
        Evento e = evento();
        e.setEstado(EstadoEvento.APROBADO);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));

        mockMvc.perform(get("/eventos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));
    }

    @Test
    void getId_rechazado_invitado_404() throws Exception {
        Evento e = eventoDeOrg(EstadoEvento.RECHAZADO);
        e.setMotivoRechazo("Falta información");
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));

        mockMvc.perform(get("/eventos/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getId_rechazado_otroOrganizador_404() throws Exception {
        Evento e = eventoDeOrg(EstadoEvento.RECHAZADO);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));

        mockMvc.perform(get("/eventos/1").with(otroOrganizador()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getId_rechazado_dueno_veCompleto() throws Exception {
        Evento e = eventoDeOrg(EstadoEvento.RECHAZADO);
        e.setMotivoRechazo("Falta información");
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));

        mockMvc.perform(get("/eventos/1").with(organizador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.motivoRechazo").value("Falta información"));
    }

    @Test
    void getId_pendiente_admin_veCompleto() throws Exception {
        Evento e = eventoDeOrg(EstadoEvento.PENDIENTE_REVISION);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));

        mockMvc.perform(get("/eventos/1").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_REVISION"));
    }

    // ------------------------------------------------- 016 (Phase 4): redes sociales y contacto
    // T161: crear con 2 redes + teléfono + URL → 201 y TODO queda guardado en la entidad.
    @Test
    void post_admin_conDosRedesTelefonoYUrl_201_yGuardaTodos() throws Exception {
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        cuandoExisteEtiquetaOtros();
        String json = """
                {"establecimiento":"La Sala","direccion":"Calle 1",
                 "fecha":"2026-10-01","nombre":"Concierto de otoño","descripcion":"Música en vivo",
                 "redesSociales":[{"red":"FACEBOOK","url":"https://facebook.com/laSala"},
                                  {"red":"INSTAGRAM","url":"https://instagram.com/laSala"}],
                 "telefonoEvento":"926 42 00 00","urlEvento":"https://www.lasala.es",
                 "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}""";

        mockMvc.perform(multipart("/eventos").file(parteEvento(json)).file(imagenJpeg()).with(admin()))
                .andExpect(status().isCreated());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        Evento guardado = captor.getValue();
        assertEquals(2, guardado.getRedesSociales().size());
        assertTrue(guardado.getRedesSociales().stream().anyMatch(
                r -> r.getRed() == RedSocial.FACEBOOK && "https://facebook.com/laSala".equals(r.getUrl())));
        assertTrue(guardado.getRedesSociales().stream().anyMatch(
                r -> r.getRed() == RedSocial.INSTAGRAM && "https://instagram.com/laSala".equals(r.getUrl())));
        assertEquals("926 42 00 00", guardado.getTelefonoEvento());
        assertEquals("https://www.lasala.es", guardado.getUrlEvento());
    }

    // T162: un valor de red que no existe en el enum → 400 (lo lanza Jackson al
    // deserializar el part "evento", antes de tocar el controller).
    @Test
    void post_admin_conRedInvalida_400() throws Exception {
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        cuandoExisteEtiquetaOtros();
        String json = """
                {"establecimiento":"La Sala","direccion":"Calle 1",
                 "fecha":"2026-10-01","nombre":"Concierto de otoño","descripcion":"Música en vivo",
                 "redesSociales":[{"red":"TIKTOKK","url":"https://tiktok.com/laSala"}],
                 "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}""";

        mockMvc.perform(multipart("/eventos").file(parteEvento(json)).file(imagenJpeg()).with(admin()))
                .andExpect(status().isBadRequest());

        verify(eventoRepository, never()).save(any());
    }

    // T162b: sin redes, sin teléfono y sin URL → 201 con lista vacía y los dos null.
    @Test
    void post_admin_sinRedesNiContacto_201_conVacios() throws Exception {
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        cuandoExisteEtiquetaOtros();

        mockMvc.perform(multipart("/eventos").file(parteEvento(VALIDO)).file(imagenJpeg()).with(admin()))
                .andExpect(status().isCreated());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertTrue(captor.getValue().getRedesSociales().isEmpty());
        assertNull(captor.getValue().getTelefonoEvento());
        assertNull(captor.getValue().getUrlEvento());
    }

    // T163: la misma red repetida en la petición REEMPLAZA (último gana), sin duplicar fila.
    @Test
    void post_admin_conRedDuplicada_laReemplaza() throws Exception {
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        cuandoExisteEtiquetaOtros();
        String json = """
                {"establecimiento":"La Sala","direccion":"Calle 1",
                 "fecha":"2026-10-01","nombre":"Concierto de otoño","descripcion":"Música en vivo",
                 "redesSociales":[{"red":"FACEBOOK","url":"https://facebook.com/versionVieja"},
                                  {"red":"FACEBOOK","url":"https://facebook.com/versionNueva"},
                                  {"red":"X","url":"https://x.com/laSala"}],
                 "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}""";

        mockMvc.perform(multipart("/eventos").file(parteEvento(json)).file(imagenJpeg()).with(admin()))
                .andExpect(status().isCreated());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        Evento guardado = captor.getValue();
        assertEquals(2, guardado.getRedesSociales().size(), "dos redes distintas, no la repetida");
        assertEquals("https://facebook.com/versionNueva", guardado.getRedesSociales().stream()
                .filter(r -> r.getRed() == RedSocial.FACEBOOK).findFirst().orElseThrow().getUrl());
    }

    // T160b: en update las redes se reemplazan por las nuevas (no se acumulan).
    @Test
    void put_admin_cambiaRedes_201_ReemplazaLasAnteriores() throws Exception {
        Evento existente = evento();
        existente.setRedesSociales(new java.util.ArrayList<>(java.util.List.of(
                new com.eventos.culturales.entities.RedSocialEvento(RedSocial.YOUTUBE, "https://youtube.com/old"))));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
        cuandoExisteEtiquetaOtros();
        String json = """
                {"establecimiento":"La Sala","direccion":"Calle 1",
                 "fecha":"2026-10-01","nombre":"Concierto de otoño","descripcion":"Música en vivo",
                 "redesSociales":[{"red":"INSTAGRAM","url":"https://instagram.com/laSala"}],
                 "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}""";

        mockMvc.perform(putEvento(1L, json, admin()).file(imagenJpeg()))
                .andExpect(status().isOk());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        Evento guardado = captor.getValue();
        assertEquals(1, guardado.getRedesSociales().size());
        assertEquals(RedSocial.INSTAGRAM, guardado.getRedesSociales().get(0).getRed());
    }

    private void cuandoExisteEtiquetaOtros() {
        when(etiquetaRepository.findByNombre("OTROS"))
                .thenReturn(Optional.of(new com.eventos.culturales.entities.Etiqueta(7L, "OTROS")));
    }
}
