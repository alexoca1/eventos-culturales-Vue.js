package com.eventos.culturales.controller;

import com.eventos.culturales.dto.EventoDTO;
import com.eventos.culturales.dto.RechazoRequest;
import com.eventos.culturales.entities.CategoriaEvento;
import com.eventos.culturales.entities.EstadoEvento;
import com.eventos.culturales.entities.Evento;
import com.eventos.culturales.entities.Favorito;
import com.eventos.culturales.entities.Usuario;
import com.eventos.culturales.repositories.EventoRepository;
import com.eventos.culturales.repositories.FavoritoRepository;
import com.eventos.culturales.repositories.UsuarioRepository;
import com.eventos.culturales.services.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class EventoController {

    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final FavoritoRepository favoritoRepository;
    private final EmailService emailService;

    // Público (cadena 1 de SecurityConfig): ?fecha= y ?categoria= opcionales y combinables.
    // ?fecha= devuelve los eventos VIGENTES ese día (rango [fecha, fechaFin]); sin filtros, todos ordenados.
    // 007: quien no sea ROLE_ADMIN solo ve APROBADO (en todas las ramas).
    @GetMapping("/eventos")
    public ResponseEntity<List<Evento>> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) CategoriaEvento categoria,
            Authentication authentication) {
        if (isAdmin(authentication)) {
            if (fecha != null && categoria != null) {
                return ResponseEntity.ok(eventoRepository.findVigentesEnPorCategoria(fecha, categoria));
            }
            if (fecha != null) {
                return ResponseEntity.ok(eventoRepository.findVigentesEn(fecha));
            }
            if (categoria != null) {
                return ResponseEntity.ok(eventoRepository.findByCategoriaOrderByIdAsc(categoria));
            }
            return ResponseEntity.ok(eventoRepository.findAllByOrderByFechaAscIdAsc());
        }
        if (fecha != null && categoria != null) {
            return ResponseEntity.ok(eventoRepository.findVigentesEnPorEstadoYCategoria(
                    EstadoEvento.APROBADO, fecha, categoria));
        }
        if (fecha != null) {
            return ResponseEntity.ok(eventoRepository.findVigentesEnPorEstado(EstadoEvento.APROBADO, fecha));
        }
        if (categoria != null) {
            return ResponseEntity.ok(eventoRepository.findByEstadoAndCategoriaOrderByIdAsc(
                    EstadoEvento.APROBADO, categoria));
        }
        return ResponseEntity.ok(eventoRepository.findByEstadoOrderByFechaAscIdAsc(EstadoEvento.APROBADO));
    }

    // Público (cadena 1 de SecurityConfig), pero un evento no APROBADO solo lo ven
    // el admin y el organizador dueño (010: no se confirma que el id existe → 404)
    @GetMapping("/eventos/{id}")
    public ResponseEntity<Evento> findById(@PathVariable Long id, Authentication authentication) {
        Evento evento = eventoRepository.findById(id).orElse(null);
        if (evento == null) {
            return ResponseEntity.notFound().build();
        }
        boolean dueno = authentication != null
                && evento.getCreadoPor() != null
                && evento.getCreadoPor().getEmail() != null
                && evento.getCreadoPor().getEmail().equals(authentication.getName());
        if (evento.getEstado() != EstadoEvento.APROBADO && !isAdmin(authentication) && !dueno) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(evento);
    }

    @PostMapping(value = "/eventos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR')")
    public ResponseEntity<?> createEvento(@Valid @RequestPart("evento") EventoDTO dto,
                                          @RequestPart(value = "file", required = false) MultipartFile file,
                                          @RequestPart(value = "fileHd", required = false) MultipartFile fileHd,
                                          Authentication authentication) {
        ResponseEntity<?> error = validarHorario(dto);
        if (error != null) return error;
        error = validarFechas(dto);
        if (error != null) return error;
        error = validarArchivo(file);
        if (error != null) return error;
        error = validarArchivo(fileHd);
        if (error != null) return error;
        Evento evento = new Evento();
        aplicar(dto, file, fileHd, evento);
        if (isAdmin(authentication)) {
            // El admin aplica directo: APROBADO sin 48h ni revisión (comportamiento actual)
            evento.setEstado(EstadoEvento.APROBADO);
            evento.setCreadoPor(usuarioActual(authentication));
        } else {
            // Organizador: 48h de antelación y queda pendiente de revisión
            Usuario organizador = usuarioActual(authentication);
            if (organizador == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Usuario autenticado no encontrado"));
            }
            if (LocalDateTime.of(dto.fecha(),
                    dto.horaInicio() != null ? dto.horaInicio() : java.time.LocalTime.MIDNIGHT)
                    .isBefore(LocalDateTime.now().plusHours(48))) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Los eventos deben crearse con al menos 48h de antelación"));
            }
            evento.setEstado(EstadoEvento.PENDIENTE_REVISION);
            evento.setCreadoPor(organizador);
        }
        evento.setMotivoRechazo(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoRepository.save(evento));
    }

    @PutMapping(value = "/eventos/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR')")
    public ResponseEntity<?> updateEvento(@Valid @RequestPart("evento") EventoDTO dto,
                                          @RequestPart(value = "file", required = false) MultipartFile file,
                                          @RequestPart(value = "fileHd", required = false) MultipartFile fileHd,
                                          @PathVariable Long id,
                                          Authentication authentication) {
        ResponseEntity<?> error = validarHorario(dto);
        if (error != null) return error;
        error = validarFechas(dto);
        if (error != null) return error;
        error = validarArchivo(file);
        if (error != null) return error;
        error = validarArchivo(fileHd);
        if (error != null) return error;
        return eventoRepository.findById(id)
                .map(evento -> {
                    if (isAdmin(authentication)) {
                        // El admin aplica directo: queda APROBADO (comportamiento actual)
                        aplicar(dto, file, fileHd, evento);
                        evento.setEstado(EstadoEvento.APROBADO);
                        evento.setMotivoRechazo(null);
                        return ResponseEntity.ok(eventoRepository.save(evento));
                    }
                    // Organizador: solo el dueño, y nunca lo pendiente de borrar
                    if (evento.getCreadoPor() == null
                            || !evento.getCreadoPor().getEmail().equals(authentication.getName())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "Solo puedes editar tus propios eventos"));
                    }
                    if (evento.getEstado() == EstadoEvento.PENDIENTE_ELIMINACION) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "No se puede editar un evento pendiente de eliminación"));
                    }
                    aplicar(dto, file, fileHd, evento);
                    evento.setEstado(EstadoEvento.PENDIENTE_REVISION);
                    return ResponseEntity.ok(eventoRepository.save(evento));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/eventos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR')")
    public ResponseEntity<?> deleteEvento(@PathVariable Long id, Authentication authentication) {
        return eventoRepository.findById(id)
                .map(evento -> {
                    if (isAdmin(authentication)) {
                        // El admin borra directamente (comportamiento actual, sin cambios)
                        // 008 US3: primero los favoritos para no romper integridad referencial
                        favoritoRepository.deleteByEvento(evento);
                        eventoRepository.deleteById(id);
                        return ResponseEntity.noContent().build();
                    }
                    // Organizador: solo el dueño, y pedir eliminación (no borrar)
                    if (evento.getCreadoPor() == null
                            || !evento.getCreadoPor().getEmail().equals(authentication.getName())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "Solo puedes eliminar tus propios eventos"));
                    }
                    evento.setEstado(EstadoEvento.PENDIENTE_ELIMINACION);
                    return ResponseEntity.ok(eventoRepository.save(evento));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Borrado múltiple (reservado): DELETE /eventos?ids=1,2,3
    @DeleteMapping("/eventos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEventos(@RequestParam List<Long> ids) {
        eventoRepository.deleteAllById(ids);
        return ResponseEntity.noContent().build();
    }

    // Cartel público (cadena 1 de SecurityConfig: los <img> no mandan Authorization).
    // Si hay imagen en BD se sirve; si no, se redirige a la URL externa; si no hay nada, 404.
    @GetMapping("/eventos/{id}/cartel")
    public ResponseEntity<?> cartel(@PathVariable Long id) {
        return eventoRepository.findById(id)
                .map(evento -> {
                    if (evento.getCartel() != null && evento.getCartel().length > 0) {
                        return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(evento.getCartelContentType()))
                                .body(evento.getCartel());
                    }
                    if (evento.getCartelUrl() != null && !evento.getCartelUrl().isBlank()) {
                        return ResponseEntity.status(HttpStatus.FOUND)
                                .location(URI.create(evento.getCartelUrl())).build();
                    }
                    return ResponseEntity.notFound().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Cartel HD público (lightbox). Misma lógica que /cartel.
    @GetMapping("/eventos/{id}/cartel-hd")
    public ResponseEntity<?> cartelHd(@PathVariable Long id) {
        return eventoRepository.findById(id)
                .map(evento -> {
                    if (evento.getCartelHd() != null && evento.getCartelHd().length > 0) {
                        return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(evento.getCartelHdContentType()))
                                .body(evento.getCartelHd());
                    }
                    if (evento.getCartelUrl() != null && !evento.getCartelUrl().isBlank()) {
                        return ResponseEntity.status(HttpStatus.FOUND)
                                .location(URI.create(evento.getCartelUrl())).build();
                    }
                    return ResponseEntity.notFound().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // JPEG/PNG/WebP (los GIF no se admiten); el frontend ya redimensiona antes de subir,
    // el tope del backend es solo red de seguridad ante subidas directas a la API.
    private static final Set<String> TIPOS_CARTEL = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_CARTEL_BYTES = 2 * 1024 * 1024;

    private void aplicar(EventoDTO dto, MultipartFile file, MultipartFile fileHd, Evento evento) {
        evento.setEstablecimiento(dto.establecimiento());
        evento.setDireccion(dto.direccion());
        evento.setFecha(dto.fecha());
        evento.setDescripcion(dto.descripcion());
        evento.setCartelUrl(dto.cartelUrl());
        evento.setHoraInicio(dto.horaInicio());
        evento.setHoraFin(dto.horaFin());
        // Sin fecha fin = un día (se guarda fecha para no tener dos fuentes de verdad)
        evento.setFechaFin(dto.fechaFin() != null ? dto.fechaFin() : dto.fecha());
        evento.setCategoria(dto.categoria());
        evento.setMapaEmbed(dto.mapaEmbed());
        if (file != null && !file.isEmpty()) {
            try {
                evento.setCartel(file.getBytes());
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer el cartel subido", e);
            }
            evento.setCartelContentType(file.getContentType());
        }
        if (fileHd != null && !fileHd.isEmpty()) {
            try {
                evento.setCartelHd(fileHd.getBytes());
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer el cartel HD subido", e);
            }
            evento.setCartelHdContentType(fileHd.getContentType());
        }
    }

    // US2 004: el horario se informa completo o no se informa (evita datos a medias)
    private ResponseEntity<?> validarHorario(EventoDTO dto) {
        if ((dto.horaInicio() == null) != (dto.horaFin() == null)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "horaInicio y horaFin deben informarse juntas"));
        }
        return null;
    }

    // 006: la fecha fin no puede ser anterior a la de inicio
    private ResponseEntity<?> validarFechas(EventoDTO dto) {
        if (dto.fechaFin() != null && dto.fecha() != null && dto.fechaFin().isBefore(dto.fecha())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La fecha de fin no puede ser anterior a la de inicio"));
        }
        return null;
    }

    // 007 US6: panel del organizador (solo sus eventos, en cualquier estado)
    @GetMapping("/eventos/mios")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public ResponseEntity<?> mios(Authentication authentication) {
        Usuario organizador = usuarioActual(authentication);
        if (organizador == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario autenticado no encontrado"));
        }
        return ResponseEntity.ok(eventoRepository.findByCreadoPorOrderByIdAsc(organizador));
    }

    // 007 US2: cola de moderación (revisiones + eliminaciones pendientes), solo admin
    @GetMapping("/eventos/pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Evento>> pendientes() {
        return ResponseEntity.ok(eventoRepository.findByEstadoInOrderByIdAsc(
                List.of(EstadoEvento.PENDIENTE_REVISION, EstadoEvento.PENDIENTE_ELIMINACION)));
    }

    // 007 US2: aprobar revisión → APROBADO; aprobar eliminación → borrado definitivo
    @PostMapping("/eventos/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> aprobar(@PathVariable Long id) {
        return eventoRepository.findById(id)
                .map(evento -> {
                    if (evento.getEstado() == EstadoEvento.PENDIENTE_ELIMINACION) {
                        // 008 US3: limpiar favoritos antes del borrado definitivo
                        favoritoRepository.deleteByEvento(evento);
                        eventoRepository.delete(evento);
                        return ResponseEntity.noContent().build();
                    }
                    if (evento.getEstado() != EstadoEvento.PENDIENTE_REVISION) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "Solo se puede aprobar un evento pendiente"));
                    }
                    evento.setEstado(EstadoEvento.APROBADO);
                    evento.setMotivoRechazo(null);
                    Evento guardado = eventoRepository.save(evento);
                    notificar(evento, "Tu evento ha sido APROBADO",
                            "Tu evento '" + evento.getDescripcion() + "' ya es visible en la web.");
                    return ResponseEntity.ok(guardado);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 007 US2: rechazar revisión → RECHAZADO (guarda motivo); rechazar eliminación → vuelve a APROBADO
    @PostMapping("/eventos/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rechazar(@PathVariable Long id,
                                      @RequestBody(required = false) RechazoRequest body) {
        return eventoRepository.findById(id)
                .map(evento -> {
                    if (evento.getEstado() == EstadoEvento.PENDIENTE_ELIMINACION) {
                        evento.setEstado(EstadoEvento.APROBADO);
                        return ResponseEntity.ok(eventoRepository.save(evento));
                    }
                    if (evento.getEstado() != EstadoEvento.PENDIENTE_REVISION) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "Solo se puede rechazar un evento pendiente"));
                    }
                    evento.setEstado(EstadoEvento.RECHAZADO);
                    evento.setMotivoRechazo(body != null ? body.motivo() : null);
                    Evento guardado = eventoRepository.save(evento);
                    String cuerpo = "Tu evento '" + evento.getDescripcion() + "' ha sido rechazado."
                            + (evento.getMotivoRechazo() != null && !evento.getMotivoRechazo().isBlank()
                                    ? " Motivo: " + evento.getMotivoRechazo() : "");
                    notificar(evento, "Tu evento ha sido RECHAZADO", cuerpo);
                    return ResponseEntity.ok(guardado);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 007 US6: notifica al dueño si hay email; el envío nunca revierte nada (FR-013).
    // Doble blindaje: EmailService ya traga sus excepciones, pero este try/catch protege
    // además contra cualquier implementación futura de EmailService que sí propague.
    private void notificar(Evento evento, String asunto, String cuerpo) {
        try {
            if (evento.getCreadoPor() == null || evento.getCreadoPor().getEmail() == null) return;
            emailService.enviar(evento.getCreadoPor().getEmail(), asunto, cuerpo);
        } catch (Exception e) {
            System.err.println("[EventoController] Fallo notificando evento " + evento.getId() + ": " + e.getMessage());
        }
    }

    // 008 US2: mis favoritos que siguen APROBADO (el resto se oculta, pero sigue en BD)
    // OJO: "/eventos/favoritos" literal gana a "/eventos/{id}" en el matching de Spring MVC
    @GetMapping("/eventos/favoritos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> misFavoritos(Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario autenticado no encontrado"));
        }
        List<Evento> eventos = favoritoRepository.findByUsuarioAndEventoEstado(usuario, EstadoEvento.APROBADO)
                .stream()
                .map(Favorito::getEvento)
                .toList();
        return ResponseEntity.ok(eventos);
    }

    // 008 US1: marcar favorito (idempotente; solo sobre APROBADO para no exponer otros estados)
    @PostMapping("/eventos/{id}/favorito")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> marcarFavorito(@PathVariable Long id, Authentication authentication) {
        return eventoRepository.findById(id)
                .filter(e -> e.getEstado() == EstadoEvento.APROBADO)
                .map(evento -> {
                    Usuario usuario = usuarioActual(authentication);
                    if (usuario == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error", "Usuario autenticado no encontrado"));
                    }
                    if (favoritoRepository.findByUsuarioAndEvento(usuario, evento).isEmpty()) {
                        Favorito favorito = new Favorito();
                        favorito.setUsuario(usuario);
                        favorito.setEvento(evento);
                        favoritoRepository.save(favorito);
                        return ResponseEntity.status(HttpStatus.CREATED).build();
                    }
                    return ResponseEntity.ok().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 008 US1: desmarcar favorito (idempotente: no falla si no existía)
    @DeleteMapping("/eventos/{id}/favorito")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> desmarcarFavorito(@PathVariable Long id, Authentication authentication) {
        return eventoRepository.findById(id)
                .map(evento -> {
                    Usuario usuario = usuarioActual(authentication);
                    if (usuario == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error", "Usuario autenticado no encontrado"));
                    }
                    favoritoRepository.findByUsuarioAndEvento(usuario, evento)
                            .ifPresent(favoritoRepository::delete);
                    return ResponseEntity.noContent().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 007: helpers de rol y usuario autenticado
    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private Usuario usuarioActual(Authentication authentication) {
        if (authentication == null) return null;
        return usuarioRepository.findByEmail(authentication.getName()).orElse(null);
    }

    // El mapa se valida en EventoDTO (@Pattern estricto); aquí solo los ficheros.
    private ResponseEntity<?> validarArchivo(MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            if (!TIPOS_CARTEL.contains(file.getContentType())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El cartel debe ser JPEG, PNG o WebP (los GIF no se admiten)"));
            }
            if (file.getSize() > MAX_CARTEL_BYTES) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El cartel no puede superar 2 MB"));
            }
        }
        return null;
    }
}
