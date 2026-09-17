package com.eventos.culturales.controller;

import com.eventos.culturales.entities.Evento;
import com.eventos.culturales.repositories.EventoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sin token: la consulta pública por fecha debe funcionar (cadena 1 de SecurityConfig)
@WebMvcTest(EventoController.class)
class EventoControllerPublicTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventoRepository eventoRepository;

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

    @Test
    void getPorFecha_devuelveTodosLosDelDia() throws Exception {
        when(eventoRepository.findByFechaOrderByIdAsc(LocalDate.parse("2026-10-01")))
                .thenReturn(List.of(evento(1L, "2026-10-01"), evento(2L, "2026-10-01")));

        mockMvc.perform(get("/eventos").param("fecha", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].establecimiento").value("El mesoncito"));
    }

    @Test
    void getPorFechaSinEventos_devuelveListaVacia() throws Exception {
        when(eventoRepository.findByFechaOrderByIdAsc(LocalDate.parse("2026-10-02")))
                .thenReturn(List.of());

        mockMvc.perform(get("/eventos").param("fecha", "2026-10-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getSinFecha_devuelveTodos() throws Exception {
        when(eventoRepository.findAllByOrderByFechaAscIdAsc())
                .thenReturn(List.of(evento(1L, "2026-10-01")));

        mockMvc.perform(get("/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getPorIdInexistente_devuelve404() throws Exception {
        when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/eventos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getConFechaInvalida_devuelve400() throws Exception {
        mockMvc.perform(get("/eventos").param("fecha", "no-es-fecha"))
                .andExpect(status().isBadRequest());
    }
}
