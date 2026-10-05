package com.eventos.culturales.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // Manejo de errores de validación (@Blanck, @Email, etc. en entidades y @Valid en controladores)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();

        //Guardamos en el Map errores todos los campos y mensajes de error que han fallado
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errores.put(error.getField(), error.getDefaultMessage());
        });
        //Devolvemos el Map que se transforma en un JSON automáticamente
        return ResponseEntity.badRequest().body(errores);
    }


    // Fichero que supera spring.servlet.multipart.max-* → 400, no 500
    @ExceptionHandler(org.springframework.web.multipart.MultipartException.class)
    public ResponseEntity<Map<String, String>> handleMultipart(org.springframework.web.multipart.MultipartException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "El fichero supera el tamaño máximo permitido (2 MB)"));
    }

    // Manejo de acceso denegado por Spring Security (@PreAuthorize, etc.)
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Acceso denegado: no tienes permisos suficientes"));
    }

    // Parámetro con formato inválido (ej: ?fecha=no-es-fecha) → 400, no 500
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Parámetro '" + ex.getName() + "' con formato inválido"));
    }

    // JSON malformado o valor fuera de lista fija (ej: categoria inexistente) → 400, no 500
    // (este handler tiene prioridad sobre el genérico Exception.class)
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Cuerpo de la petición inválido (revisa enums, fechas y horas)"));
    }

    // El INSERT de un LONGBLOB viaja en un paquete MariaDB de ~2x los bytes de la foto:
    // con el max_allowed_packet por defecto (1 MB) una foto de >= 512 KB revienta con un
    // 500 sin pista. Se traduce a 413 con el motivo real; el resto de fallos de JPA caen
    // en el 500 genérico de abajo como hasta ahora.
    @ExceptionHandler(org.springframework.orm.jpa.JpaSystemException.class)
    public ResponseEntity<Map<String, String>> handleJpaSystemException(
            org.springframework.orm.jpa.JpaSystemException ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains("Packet for query is too large")) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body(Map.of("error", "La imagen supera el tamaño máximo que admite la base de datos; "
                                + "reduce su resolución o aumenta max_allowed_packet del servidor MySQL"));
            }
        }
        return handleGlobalException(ex);
    }

    // Manejo de excepciones no controladas: al cliente SIEMPRE mensaje genérico;
    // en el servidor solo se registra la clase y el mensaje, sin stack trace: la traza
    // completa puede volcar payloads con datos personales (US2 hardening + RGPD).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGlobalException(Exception ex) {
        System.err.println("[GlobalExceptionHandler] Excepción no controlada: "
                + ex.getClass().getName() + " — " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno del servidor"));
    }
}
