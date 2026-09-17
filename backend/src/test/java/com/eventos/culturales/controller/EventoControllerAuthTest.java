package com.eventos.culturales.controller;

import com.eventos.culturales.config.JwtSecretKeyProvider;
import com.eventos.culturales.config.SecurityConfig;
import com.eventos.culturales.entities.Evento;
import com.eventos.culturales.repositories.EventoRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

    // @EnableJpaAuditing exige JpaMappingContext, ausente en el slice @WebMvcTest
    @MockitoBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static RequestPostProcessor admin() {
        return jwt().authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private static RequestPostProcessor user() {
        return jwt().authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private static final String VALIDO = """
            {"establecimiento":"El mesoncito","direccion":"C. Aduana, 3, Puertollano",
             "fecha":"2026-10-01","descripcion":"Fiesta Mexicana",
             "cartelUrl":"",
             "mapaEmbed":"<iframe src=\\"https://www.google.com/maps/embed?pb=xyz\\"></iframe>"}""";

    private static final String SIN_ESTABLECIMIENTO = """
            {"establecimiento":"","direccion":"C. Aduana, 3, Puertollano",
             "fecha":"2026-10-01","descripcion":"Fiesta Mexicana"}""";

    private static final String MAPA_MALO = """
            {"establecimiento":"El mesoncito","direccion":"C. Aduana, 3, Puertollano",
             "fecha":"2026-10-01","descripcion":"Fiesta Mexicana",
             "mapaEmbed":"<iframe src=\\"https://evil.com/map\\"></iframe>"}""";

    private static MockMultipartFile parteEvento(String json) {
        return new MockMultipartFile("evento", "", "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile imagenJpeg() {
        return new MockMultipartFile("file", "cartel.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    private Evento evento() {
        Evento e = new Evento();
        e.setId(1L);
        e.setEstablecimiento("El mesoncito");
        e.setDireccion("C. Aduana, 3, Puertollano");
        e.setFecha(LocalDate.parse("2026-10-01"));
        e.setDescripcion("Fiesta Mexicana");
        return e;
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
        MockMultipartFile hd = new MockMultipartFile("fileHd", "cartel-hd.webp", "image/webp", new byte[]{4, 5, 6});

        mockMvc.perform(multipart("/eventos").file(parteEvento(VALIDO)).file(imagenJpeg()).file(hd).with(admin()))
                .andExpect(status().isCreated());

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertArrayEquals(new byte[]{1, 2, 3}, captor.getValue().getCartel());
        assertEquals("image/jpeg", captor.getValue().getCartelContentType());
        assertArrayEquals(new byte[]{4, 5, 6}, captor.getValue().getCartelHd());
        assertEquals("image/webp", captor.getValue().getCartelHdContentType());
    }

    @Test
    void post_admin_sinImagen_201() throws Exception {
        when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(multipart("/eventos").file(parteEvento(VALIDO)).with(admin()))
                .andExpect(status().isCreated());
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
}
