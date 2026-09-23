package com.eventos.culturales.config;

import com.eventos.culturales.controller.EventoController;
import com.eventos.culturales.repositories.EventoRepository;
import com.eventos.culturales.repositories.UsuarioRepository;
import com.eventos.culturales.repositories.FavoritoRepository;
import com.eventos.culturales.services.EmailService;
import com.eventos.culturales.services.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// US2: con la configuración CORS por defecto, el origen "null" (file://)
// NO debe recibir Access-Control-Allow-Origin; localhost sí.
@WebMvcTest(EventoController.class)
@Import({SecurityConfig.class, JwtService.class, JwtSecretKeyProvider.class})
class CorsDefaultConfigTest {

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

    // @EnableJpaAuditing exige JpaMappingContext, ausente en el slice @WebMvcTest
    @MockitoBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void origenNull_noRecibeAccessControlAllowOrigin() throws Exception {
        // Origen no permitido: Spring rechaza la petición CORS con 403, sin cabecera ACAO
        mockMvc.perform(get("/eventos").header("Origin", "null"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void origenLocalhost_siguePermitido() throws Exception {
        mockMvc.perform(get("/eventos").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
