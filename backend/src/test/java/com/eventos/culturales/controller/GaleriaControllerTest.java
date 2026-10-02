package com.eventos.culturales.controller;

import com.eventos.culturales.config.JwtSecretKeyProvider;
import com.eventos.culturales.config.SecurityConfig;
import com.eventos.culturales.entities.Evento;
import com.eventos.culturales.entities.FotoGaleria;
import com.eventos.culturales.entities.Usuario;
import com.eventos.culturales.repositories.EtiquetaRepository;
import com.eventos.culturales.repositories.EventoRepository;
import com.eventos.culturales.repositories.FavoritoRepository;
import com.eventos.culturales.repositories.FotoGaleriaRepository;
import com.eventos.culturales.repositories.UsuarioRepository;
import com.eventos.culturales.services.EmailService;
import com.eventos.culturales.services.JwtService;
import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.orm.jpa.JpaSystemException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 015 US2: galería de fotos. Los GET son públicos (sin post-processor de token) y los
 * POST/DELETE exigen JWT + admin o dueño.
 */
@WebMvcTest(EventoController.class)
@Import({SecurityConfig.class, JwtService.class, JwtSecretKeyProvider.class})
class GaleriaControllerTest {

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
    private EtiquetaRepository etiquetaRepository;
    @MockitoBean
    private FotoGaleriaRepository fotoGaleriaRepository;

    // @EnableJpaAuditing exige JpaMappingContext, ausente en el slice @WebMvcTest
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    // ---------------------------------------------------------------- POST

    @Test
    void post_galeria_orden0_devuelve201() throws Exception {
        givenEventoDeAdmin();
        when(fotoGaleriaRepository.findByEventoIdAndOrden(1L, 0)).thenReturn(Optional.empty());
        when(fotoGaleriaRepository.countDistinctOrdenByEventoId(1L)).thenReturn(0L);

        mockMvc.perform(multipart("/eventos/1/galeria").file(foto()).param("orden", "0").with(admin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orden").value(0));

        ArgumentCaptor<FotoGaleria> captor = ArgumentCaptor.forClass(FotoGaleria.class);
        verify(fotoGaleriaRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getOrden());
        assertArrayEquals(new byte[]{1, 2, 3}, captor.getValue().getDatos());
        assertEquals("image/jpeg", captor.getValue().getContentType());
    }

    // Reemplazar una posición ocupada NO suma al contador de 5 (FR-009).
    @Test
    void post_galeria_reemplazaOrden0_devuelve200() throws Exception {
        givenEventoDeAdmin();
        FotoGaleria previa = fotoGuardada(1L, 0, new byte[]{9});
        when(fotoGaleriaRepository.findByEventoIdAndOrden(1L, 0)).thenReturn(Optional.of(previa));

        mockMvc.perform(multipart("/eventos/1/galeria").file(foto()).param("orden", "0").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orden").value(0));

        ArgumentCaptor<FotoGaleria> captor = ArgumentCaptor.forClass(FotoGaleria.class);
        verify(fotoGaleriaRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getId(), "debe reusar la fila existente, no crear otra");
        assertArrayEquals(new byte[]{1, 2, 3}, captor.getValue().getDatos());
    }

    // Caso límite de FR-009: con las 5 posiciones ocupadas, una posición ya ocupada
    // se puede seguir reemplazando; solo una posición NUEVA choca con el tope.
    @Test
    void post_galeria_conCincoOcupadas_reemplazarSigueValiendo() throws Exception {
        givenEventoDeAdmin();
        when(fotoGaleriaRepository.findByEventoIdAndOrden(1L, 2)).thenReturn(Optional.of(fotoGuardada(1L, 2, new byte[]{9})));
        when(fotoGaleriaRepository.countDistinctOrdenByEventoId(1L)).thenReturn(5L);

        mockMvc.perform(multipart("/eventos/1/galeria").file(foto()).param("orden", "2").with(admin()))
                .andExpect(status().isOk());
    }

    @Test
    void post_galeria_sextaFotoEnPosicionNueva_devuelve400() throws Exception {
        givenEventoDeAdmin();
        when(fotoGaleriaRepository.findByEventoIdAndOrden(1L, 3)).thenReturn(Optional.empty());
        when(fotoGaleriaRepository.countDistinctOrdenByEventoId(1L)).thenReturn(5L);

        mockMvc.perform(multipart("/eventos/1/galeria").file(foto()).param("orden", "3").with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Máximo 5 fotos por evento"));

        verify(fotoGaleriaRepository, never()).save(any());
    }

    @Test
    void post_galeria_ordenFueraDeRango_devuelve400() throws Exception {
        givenEventoDeAdmin();

        mockMvc.perform(multipart("/eventos/1/galeria").file(foto()).param("orden", "5").with(admin()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/eventos/1/galeria").file(foto()).param("orden", "-1").with(admin()))
                .andExpect(status().isBadRequest());

        verify(fotoGaleriaRepository, never()).save(any());
    }

    @Test
    void post_galeria_sinFoto_devuelve400() throws Exception {
        givenEventoDeAdmin();

        mockMvc.perform(multipart("/eventos/1/galeria").param("orden", "0").with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_galeria_organizadorQueNoEsDueño_devuelve403() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDe("otro@test.com")));

        mockMvc.perform(multipart("/eventos/1/galeria").file(foto()).param("orden", "0")
                        .with(jwt().jwt(b -> b.subject("org@test.com"))
                                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ORGANIZADOR")))))
                .andExpect(status().isForbidden());

        verify(fotoGaleriaRepository, never()).save(any());
    }

    @Test
    void post_galeria_organizadorDueño_devuelve201() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDe("org@test.com")));
        when(fotoGaleriaRepository.findByEventoIdAndOrden(1L, 0)).thenReturn(Optional.empty());
        when(fotoGaleriaRepository.countDistinctOrdenByEventoId(1L)).thenReturn(0L);

        mockMvc.perform(multipart("/eventos/1/galeria").file(foto()).param("orden", "0")
                        .with(jwt().jwt(b -> b.subject("org@test.com"))
                                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ORGANIZADOR")))))
                .andExpect(status().isCreated());
    }

    @Test
    void post_galeria_sinToken_devuelve401() throws Exception {
        mockMvc.perform(multipart("/eventos/1/galeria").file(foto()).param("orden", "0"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void post_galeria_eventoInexistente_devuelve404() throws Exception {
        when(eventoRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(multipart("/eventos/99/galeria").file(foto()).param("orden", "0").with(admin()))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- GET índices (público)

    @Test
    void get_galeria_devuelveOrdenesOcupados() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDe("admin@test.com")));
        when(fotoGaleriaRepository.findOrdenesByEventoId(1L)).thenReturn(List.of(0, 2, 4));

        mockMvc.perform(get("/eventos/1/galeria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value(0))
                .andExpect(jsonPath("$[1]").value(2))
                .andExpect(jsonPath("$[2]").value(4))
                .andExpect(jsonPath("$.length()").value(3));
    }

    // Edge case de la spec: evento sin galería → [] sin error.
    @Test
    void get_galeria_sinFotos_devuelveArrayVacio() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDe("admin@test.com")));
        when(fotoGaleriaRepository.findOrdenesByEventoId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/eventos/1/galeria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void get_galeria_eventoInexistente_devuelve404() throws Exception {
        when(eventoRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/eventos/99/galeria")).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- GET foto (público)

    @Test
    void get_galeria_foto_devuelveImagenConSuContentType() throws Exception {
        FotoGaleria png = fotoGuardada(1L, 1, new byte[]{8});
        png.setContentType("image/png");
        when(fotoGaleriaRepository.findByEventoIdAndOrden(1L, 0))
                .thenReturn(Optional.of(fotoGuardada(1L, 0, new byte[]{7, 7, 7, 7})));
        when(fotoGaleriaRepository.findByEventoIdAndOrden(1L, 1)).thenReturn(Optional.of(png));

        mockMvc.perform(get("/eventos/1/galeria/0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(new byte[]{7, 7, 7, 7}));

        mockMvc.perform(get("/eventos/1/galeria/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    void get_galeria_ordenNoOcupado_devuelve404() throws Exception {
        when(fotoGaleriaRepository.findByEventoIdAndOrden(1L, 4)).thenReturn(Optional.empty());

        mockMvc.perform(get("/eventos/1/galeria/4")).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- DELETE

    @Test
    void delete_galeria_devuelve204() throws Exception {
        givenEventoDeAdmin();
        FotoGaleria foto = fotoGuardada(1L, 3, new byte[]{5});
        when(fotoGaleriaRepository.findByEventoIdAndOrden(1L, 3)).thenReturn(Optional.of(foto));

        mockMvc.perform(delete("/eventos/1/galeria/3").with(admin()))
                .andExpect(status().isNoContent());

        verify(fotoGaleriaRepository).delete(foto);
    }

    @Test
    void delete_galeria_ordenNoOcupado_devuelve404() throws Exception {
        givenEventoDeAdmin();
        when(fotoGaleriaRepository.findByEventoIdAndOrden(1L, 3)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/eventos/1/galeria/3").with(admin()))
                .andExpect(status().isNotFound());

        verify(fotoGaleriaRepository, never()).delete(any());
    }

    @Test
    void delete_galeria_organizadorQueNoEsDueño_devuelve403() throws Exception {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDe("otro@test.com")));

        mockMvc.perform(delete("/eventos/1/galeria/3")
                        .with(jwt().jwt(b -> b.subject("org@test.com"))
                                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ORGANIZADOR")))))
                .andExpect(status().isForbidden());

        verify(fotoGaleriaRepository, never()).delete(any());
    }

    @Test
    void delete_galeria_sinToken_devuelve401() throws Exception {
        mockMvc.perform(delete("/eventos/1/galeria/3")).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- mapping

    /**
     * FR-010: la cascada al borrar el evento solo funciona si está en el lado de la
     * colección. El proyecto no tiene ningún test que arranque un contexto JPA (los
     * slices son @WebMvcTest y no hay H2 ni Testcontainers), así que aquí se fija el
     * contrato del mapping por reflexión en lugar de ejercitar la cascada de verdad.
     */
    @Test
    void mapping_coleccionDeFotos_cascadaYOrphanRemovalParaBorrarElEvento() throws Exception {
        OneToMany unoAMany = Evento.class.getDeclaredField("fotos").getAnnotation(OneToMany.class);

        assertTrue(List.of(unoAMany.cascade()).contains(CascadeType.ALL), "FR-010: cascade=ALL en la colección");
        assertTrue(unoAMany.orphanRemoval(), "FR-010: orphanRemoval en la colección");
        assertEquals("evento", unoAMany.mappedBy());
    }

    /**
     * El otro lado NO debe llevar cascada: cascade en un @ManyToOne hace que borrar la
     * foto arrastre el evento entero (DELETE /galeria/{orden}). Esto lo fija para que
     * nadie lo "corrija" después.
     */
    @Test
    void mapping_manyToOneSinCascada_borrarUnaFotoNoBorraElEvento() throws Exception {
        ManyToOne manyToOne = FotoGaleria.class.getDeclaredField("evento").getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = FotoGaleria.class.getDeclaredField("evento").getAnnotation(JoinColumn.class);

        assertFalse(List.of(manyToOne.cascade()).contains(CascadeType.REMOVE),
                "cascade=REMOVE en el @ManyToOne borraría el Evento al borrar una foto");
        assertEquals("evento_id", joinColumn.name());
        assertFalse(joinColumn.nullable());
    }

    @Test
    void mapping_indiceUnicoEventoIdOrden() {
        Table tabla = FotoGaleria.class.getAnnotation(Table.class);

        assertEquals("foto_galeria", tabla.name());
        assertEquals(1, tabla.uniqueConstraints().length);
        UniqueConstraint uc = tabla.uniqueConstraints()[0];
        assertArrayEquals(new String[]{"evento_id", "orden"}, uc.columnNames());
    }

    // ---------------------------------------------------------------- helpers

      // Un LONGBLOB que no cabe en max_allowed_packet (1 MB en XAMPP por defecto) revienta
      // el INSERT. No es un fallo del servidor: debe ser 413 explicando el motivo real, no el
      // 500 generico que no orienta a nadie. Aqui se prueba de extremo a extremo, con el
      // repositorio mockeado lanzando la misma excepcion que lanza Hibernate.
      @Test
      void post_galeria_imagenQueNoCabeEnLaBd_devuelve413() throws Exception {
          givenEventoDeAdmin();
          when(fotoGaleriaRepository.findByEventoIdAndOrden(1L, 0)).thenReturn(Optional.empty());
          when(fotoGaleriaRepository.countDistinctOrdenByEventoId(1L)).thenReturn(0L);
          when(fotoGaleriaRepository.save(any(FotoGaleria.class)))
                  .thenThrow(new JpaSystemException(new RuntimeException("could not execute statement",
                          new com.mysql.cj.jdbc.exceptions.PacketTooBigException(
                                  "Packet for query is too large (1228893 > 1048576)"))));

          mockMvc.perform(multipart("/eventos/1/galeria").file(foto()).param("orden", "0").with(admin()))
                  .andExpect(status().isPayloadTooLarge())
                  .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("max_allowed_packet")));
      }

      private void givenEventoDeAdmin() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoDe("admin@test.com")));
    }

    private static Evento eventoDe(String emailDueno) {
        Evento e = new Evento();
        e.setId(1L);
        e.setEstablecimiento("El mesoncito");
        e.setDireccion("C. Aduana, 3, Puertollano");
        e.setFecha(LocalDate.parse("2026-10-01"));
        e.setNombre("Fiesta Mexicana");
        e.setDescripcion("Fiesta Mexicana");
        e.setCreadoPor(Usuario.builder().id(2L).email(emailDueno)
                .roles(Set.of("ROLE_ORGANIZADOR")).enabled(true).build());
        return e;
    }

    private static MockMultipartFile foto() {
        return new MockMultipartFile("foto", "g.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    private static FotoGaleria fotoGuardada(Long id, int orden, byte[] datos) {
        FotoGaleria f = new FotoGaleria();
        f.setId(id);
        f.setOrden(orden);
        f.setDatos(datos);
        f.setContentType("image/jpeg");
        return f;
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
