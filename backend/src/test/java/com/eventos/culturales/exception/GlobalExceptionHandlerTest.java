package com.eventos.culturales.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.JpaSystemException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * El advice se prueba directamente, sin @WebMvcTest: es un POJO con logica pura y el
 * slice no enruta controllers anidados, asi que via MockMvc estas aserciones se
 * cumplian sobre el 404 de una ruta inexistente y no sobre la excepcion launched.
 * La cobertura de extremo a extremo del 413 vive en GaleriaControllerTest, sobre el
 * controller real.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static JpaSystemException paqueteDemasiadoGrande() {
        // Asi llega desde Hibernate: la causa real del driver va encadenada.
        return new JpaSystemException(new RuntimeException("could not execute statement",
                new com.mysql.cj.jdbc.exceptions.PacketTooBigException(
                        "Packet for query is too large (1228893 > 1048576). "
                                + "You can increase this limit by using the max_allowed_packet configuration")));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> cuerpo(org.springframework.http.ResponseEntity<Map<String, String>> r) {
        return r.getBody();
    }

    // Un LONGBLOB que no cabe en max_allowed_packet (1 MB en XAMPP) llega como
    // PacketTooBigException envuelto en JpaSystemException. No es un fallo del servidor:
    // la imagen es demasiado grande para la BD y la respuesta debe decirlo, con 413 y el
    // motivo, en vez del 500 generico que no orienta a nadie.
    @Test
    void llongobloDemasiadoGrande_devuelve413_conElMotivoReal() {
        var r = handler.handleJpaSystemException(paqueteDemasiadoGrande());

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, r.getStatusCode());
        assertEquals("La imagen supera el tamaño máximo que admite la base de datos; "
                + "reduce su resolución o aumenta max_allowed_packet del servidor MySQL",
                cuerpo(r).get("error"));
    }

    // La causa puede venir envuelta en más de un nivel (el caso real lo lleva); el
    // recorrido completo de la cadena debe encontrar el mensaje igual.
    @Test
    void encuentraElMotivoAunqueEsteProfundoEnLaCadena() {
        var conDosNiveles = new JpaSystemException(new RuntimeException("wrapper externo",
                new RuntimeException("wrapper interno", paqueteDemasiadoGrande())));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE,
                handler.handleJpaSystemException(conDosNiveles).getStatusCode());
    }

    // Un JpaSystemException que NO viene del tamaño del paquete sigue siendo un 500
    // generico: el 413 no puede tragarse fallos de JPA de cualquier otro tipo.
    @Test
    void jpaSystemExceptionDeOtroTipo_sigueSiendo500Generico() {
        var jpa = new JpaSystemException(new RuntimeException("could not execute statement",
                new IllegalStateException("deadlock detectado en la tabla usuario")));

        var r = handler.handleJpaSystemException(jpa);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.getStatusCode());
        assertEquals("Error interno del servidor", cuerpo(r).get("error"));
        assertFalse(cuerpo(r).toString().contains("deadlock"));
    }

    // US2: una excepcion no controlada MUST devolver un 500 generico, sin filtrar
    // ni el nombre de clase ni el mensaje de la excepcion original.
    @Test
    void excepcionNoControlada_devuelve500Generico_sinFiltrarDetalle() {
        var r = handler.handleGlobalException(
                new IllegalStateException("SECRETO interno: tabla usuario password=xyz"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.getStatusCode());
        assertEquals("Error interno del servidor", cuerpo(r).get("error"));

        var texto = cuerpo(r).toString();
        assertFalse(texto.contains("SECRETO"));
        assertFalse(texto.contains("IllegalStateException"));
    }
}
