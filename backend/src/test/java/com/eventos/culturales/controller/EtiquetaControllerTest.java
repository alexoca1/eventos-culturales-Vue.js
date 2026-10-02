package com.eventos.culturales.controller;

import com.eventos.culturales.config.JwtSecretKeyProvider;
import com.eventos.culturales.config.SecurityConfig;
import com.eventos.culturales.entities.Etiqueta;
import com.eventos.culturales.repositories.EtiquetaRepository;
import com.eventos.culturales.repositories.EventoRepository;
import com.eventos.culturales.services.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

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

// Punto 2: catálogo de etiquetas — GET público, mutaciones solo ADMIN
@WebMvcTest(EtiquetaController.class)
@Import({SecurityConfig.class, JwtService.class, JwtSecretKeyProvider.class})
class EtiquetaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EtiquetaRepository etiquetaRepository;

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

    @Test
    void listar_sinAuth_200() throws Exception {
        when(etiquetaRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(new Etiqueta(1L, "MUSICA")));

        mockMvc.perform(get("/etiquetas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("MUSICA"));
    }

    @Test
    void crear_admin_201() throws Exception {
        when(etiquetaRepository.findByNombre("JAZZ")).thenReturn(Optional.empty());
        when(etiquetaRepository.save(any(Etiqueta.class)))
                .thenAnswer(i -> new Etiqueta(8L, i.getArgument(0, Etiqueta.class).getNombre()));

        mockMvc.perform(post("/etiquetas").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"JAZZ\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("JAZZ"));
    }

    @Test
    void crear_sinNombre_400() throws Exception {
        mockMvc.perform(post("/etiquetas").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_duplicada_409() throws Exception {
        when(etiquetaRepository.findByNombre("MUSICA"))
                .thenReturn(Optional.of(new Etiqueta(1L, "MUSICA")));

        mockMvc.perform(post("/etiquetas").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"MUSICA\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void crear_user_403() throws Exception {
        mockMvc.perform(post("/etiquetas").with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"JAZZ\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crear_anonimo_401() throws Exception {
        mockMvc.perform(post("/etiquetas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"JAZZ\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void renombrar_admin_200() throws Exception {
        when(etiquetaRepository.findById(1L)).thenReturn(Optional.of(new Etiqueta(1L, "MUSICA")));
        when(etiquetaRepository.findByNombre("ROCK")).thenReturn(Optional.empty());
        when(etiquetaRepository.save(any(Etiqueta.class)))
                .thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(put("/etiquetas/1").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"ROCK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("ROCK"));
    }

    @Test
    void renombrar_inexistente_404() throws Exception {
        when(etiquetaRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/etiquetas/999").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"ROCK\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void renombrar_duplicada_409() throws Exception {
        when(etiquetaRepository.findById(1L)).thenReturn(Optional.of(new Etiqueta(1L, "MUSICA")));
        when(etiquetaRepository.findByNombre("TEATRO"))
                .thenReturn(Optional.of(new Etiqueta(2L, "TEATRO")));

        mockMvc.perform(put("/etiquetas/1").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"TEATRO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminar_sinUso_204() throws Exception {
        when(etiquetaRepository.findById(1L)).thenReturn(Optional.of(new Etiqueta(1L, "MUSICA")));
        when(eventoRepository.existsByEtiquetasId(1L)).thenReturn(false);

        mockMvc.perform(delete("/etiquetas/1").with(admin()))
                .andExpect(status().isNoContent());
        verify(etiquetaRepository).delete(any(Etiqueta.class));
    }

    @Test
    void eliminar_enUso_409() throws Exception {
        when(etiquetaRepository.findById(1L)).thenReturn(Optional.of(new Etiqueta(1L, "MUSICA")));
        when(eventoRepository.existsByEtiquetasId(1L)).thenReturn(true);

        mockMvc.perform(delete("/etiquetas/1").with(admin()))
                .andExpect(status().isConflict());
        verify(etiquetaRepository, never()).delete(any(Etiqueta.class));
    }

    @Test
    void eliminar_inexistente_404() throws Exception {
        when(etiquetaRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/etiquetas/999").with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_user_403() throws Exception {
        mockMvc.perform(delete("/etiquetas/1").with(user()))
                .andExpect(status().isForbidden());
    }
}
