package com.eventos.culturales.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// US2: una excepción no controlada MUST devolver un 500 genérico, sin filtrar
// ni el nombre de clase ni el mensaje de la excepción original.
@WebMvcTest(controllers = GlobalExceptionHandlerTest.BoomController.class)
class GlobalExceptionHandlerTest {

    @RestController
    static class BoomController {
        @GetMapping("/boom-solo-para-test")
        public String boom() {
            throw new IllegalStateException("SECRETO interno: tabla usuario password=xyz");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    // @EnableJpaAuditing exige JpaMappingContext, ausente en el slice @WebMvcTest
    @MockitoBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void excepcionNoControlada_devuelve500Generico_sinFiltrarDetalle() throws Exception {
        mockMvc.perform(get("/boom-solo-para-test"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error interno del servidor"))
                .andExpect(content().string(not(containsString("SECRETO"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
