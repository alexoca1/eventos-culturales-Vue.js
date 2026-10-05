// SPDX-License-Identifier: MIT

package com.eventos.culturales.controller;

import com.eventos.culturales.dto.EventoDTO;
import com.eventos.culturales.dto.EventoResponseDTO;
import com.eventos.culturales.dto.RechazoRequest;
import com.eventos.culturales.dto.RedSocialDTO;
import com.eventos.culturales.entities.EstadoEvento;
import com.eventos.culturales.entities.Etiqueta;
import com.eventos.culturales.entities.Evento;
import com.eventos.culturales.entities.Favorito;
import com.eventos.culturales.entities.FotoGaleria;
import com.eventos.culturales.entities.RedSocial;
import com.eventos.culturales.entities.RedSocialEvento;
import com.eventos.culturales.entities.Usuario;
import com.eventos.culturales.repositories.EtiquetaRepository;
import com.eventos.culturales.repositories.EventoRepository;
import com.eventos.culturales.repositories.FavoritoRepository;
import com.eventos.culturales.repositories.FotoGaleriaRepository;
import com.eventos.culturales.repositories.UsuarioRepository;
import com.eventos.culturales.services.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class EventoController {

    private final EventoRepository eventoRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final UsuarioRepository usuarioRepository;
    private final FavoritoRepository favoritoRepository;
    private final EmailService emailService;
    private final FotoGaleriaRepository fotoGaleriaRepository;

    // 014: tamaño de página fijo en servidor (?size= no se expone al cliente)
    @Value("${app.paginacion.size:10}")
    private int pageSize;

    // Público (cadena 1 de SecurityConfig): ?fecha= y ?etiquetas= opcionales y combinables.
    // ?fecha= devuelve los eventos VIGENTES ese día (rango [fecha, fechaFin]); sin filtros, todos ordenados.
    // ?etiquetas=MUSICA,TEATRO filtra con coincidencia ANY; vacío/ausente = sin filtro (igual que antes).
    // ?futuros=true (punto 6): solo vigentes hoy o después, de lo más próximo a lo último;
    // excluyente con ?fecha= (400). Combinable con ?etiquetas=.
    // 016: ?q=texto busca por nombre o establecimiento (LIKE parcial, case-insensitive).
    // Excluyente con ?fecha= y ?futuros=true (400); la longitud útil debe estar en [2, 100]
    // (400 si no). q vacío/en blanco se ignora (comportamiento actual).
    // 014: ?page=N (base 0, default 0) pagina en servidor las 9 ramas listadas (las 4 de
    // fecha exacta devuelven List completa, suelen ser pequeñas); la respuesta es Page<Evento>.
    // 007: quien no sea ROLE_ADMIN solo ve APROBADO (en todas las ramas).
    @GetMapping("/eventos")
    public ResponseEntity<?> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) List<String> etiquetas,
            @RequestParam(required = false) Boolean futuros,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication) {
        List<String> tags = etiquetasLimpias(etiquetas);
        boolean soloFuturos = Boolean.TRUE.equals(futuros);
        if (soloFuturos && fecha != null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Los parámetros fecha y futuros son excluyentes"));
        }
        java.time.LocalDate hoy = java.time.LocalDate.now();
        org.springframework.data.domain.Pageable paginacion =
                org.springframework.data.domain.PageRequest.of(page, pageSize);
        // 016: rama de búsqueda por texto (tiene prioridad sobre el resto de filtros).
        if (q != null && !q.isBlank()) {
            if (soloFuturos || fecha != null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El parámetro q es excluyente con fecha y futuros"));
            }
            String qEscapado = escaparLike(q);
            if (qEscapado.length() < 2 || qEscapado.length() > 100) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El parámetro q debe tener entre 2 y 100 caracteres"));
            }
            if (isAdmin(authentication)) {
                return ok(eventoRepository.buscarPorTexto(qEscapado, paginacion));
            }
            return ok(eventoRepository.buscarPorTextoYEstado(
                    qEscapado, EstadoEvento.APROBADO, paginacion));
        }
        if (isAdmin(authentication)) {
            if (soloFuturos) {
                return ok(tags.isEmpty()
                        ? eventoRepository.findFuturos(hoy, paginacion)
                        : eventoRepository.findFuturosPorEtiquetas(hoy, tags, paginacion));
            }
            if (fecha != null && !tags.isEmpty()) {
                return ok(eventoRepository.findVigentesEnPorEtiquetas(fecha, tags));
            }
            if (fecha != null) {
                return ok(eventoRepository.findVigentesEn(fecha));
            }
            if (!tags.isEmpty()) {
                return ok(eventoRepository.findDistinctByEtiquetasNombreInOrderByIdAsc(tags, paginacion));
            }
            return ok(eventoRepository.findAllByOrderByFechaAscIdAsc(paginacion));
        }
        if (soloFuturos) {
            return ok(tags.isEmpty()
                    ? eventoRepository.findFuturosPorEstado(EstadoEvento.APROBADO, hoy, paginacion)
                    : eventoRepository.findFuturosPorEstadoYEtiquetas(EstadoEvento.APROBADO, hoy, tags, paginacion));
        }
        if (fecha != null && !tags.isEmpty()) {
            return ok(eventoRepository.findVigentesEnPorEstadoYEtiquetas(
                    EstadoEvento.APROBADO, fecha, tags));
        }
        if (fecha != null) {
            return ok(eventoRepository.findVigentesEnPorEstado(EstadoEvento.APROBADO, fecha));
        }
        if (!tags.isEmpty()) {
            return ok(eventoRepository.findDistinctByEstadoAndEtiquetasNombreInOrderByIdAsc(
                    EstadoEvento.APROBADO, tags, paginacion));
        }
        return ok(eventoRepository.findByEstadoOrderByFechaAscIdAsc(EstadoEvento.APROBADO, paginacion));
    }

    // ?etiquetas= (o ?etiquetas sin valor) = sin filtro; el resto se usa tal cual (ANY).
    private static List<String> etiquetasLimpias(List<String> etiquetas) {
        if (etiquetas == null) return List.of();
        return etiquetas.stream().filter(s -> s != null && !s.isBlank()).toList();
    }

    // 016: escapa los comodines de LIKE para que % y _ se busquen como texto literal
    // (el \ primero, si no re-escaparía las barras que acabamos de introducir).
    private static String escaparLike(String q) {
        return q.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    // Público (cadena 1 de SecurityConfig), pero un evento no APROBADO solo lo ven
    // el admin y el organizador dueño (010: no se confirma que el id existe → 404)
    @GetMapping("/eventos/{id}")
    public ResponseEntity<EventoResponseDTO> findById(@PathVariable Long id, Authentication authentication) {
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
        return ok(evento);
    }

    @PostMapping(value = "/eventos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR')")
    public ResponseEntity<?> createEvento(@Valid @RequestPart("evento") EventoDTO dto,
                                          @RequestPart(value = "file", required = false) MultipartFile file,
                                          @RequestPart(value = "fileHd", required = false) MultipartFile fileHd,
                                          Authentication authentication) {
        // Punto 3: el cartel es obligatorio al crear (en edición se conserva el existente)
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El cartel es obligatorio al crear el evento"));
        }
        ResponseEntity<?> error = validarHorario(dto);
        if (error != null) return error;
        error = validarFechas(dto);
        if (error != null) return error;
        error = validarEtiquetas(dto);
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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EventoResponseDTO.fromEntity(eventoRepository.save(evento)));
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
        error = validarEtiquetas(dto);
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
                        return ok(eventoRepository.save(evento));
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
                    return ok(eventoRepository.save(evento));
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
                    return ok(eventoRepository.save(evento));
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

    // 015 US2: galería de hasta 5 fotos por evento (posiciones 0-4).
    // Los GET son públicos por el mismo motivo que /cartel: los <img> no mandan
    // Authorization, y el matcher de la cadena 1 ya cubre cualquier GET /eventos/**.
    private static final int MAX_FOTOS = 5;

    // FR-007: índices ocupados, no las imágenes. [] si el evento no tiene galería.
    @GetMapping("/eventos/{id}/galeria")
    public ResponseEntity<List<Integer>> ordenesGaleria(@PathVariable Long id) {
        if (eventoRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(fotoGaleriaRepository.findOrdenesByEventoId(id));
    }

    // FR-008: la foto en sí, con su Content-Type. 404 si esa posición está libre.
    @GetMapping("/eventos/{id}/galeria/{orden}")
    public ResponseEntity<?> fotoGaleria(@PathVariable Long id, @PathVariable int orden) {
        return fotoGaleriaRepository.findByEventoIdAndOrden(id, orden)
                .map(foto -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(foto.getContentType()))
                        .body(foto.getDatos()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // FR-005/FR-009: upsert por posición. Reemplazar no suma al contador, así que el
    // tope de 5 solo salta cuando el orden pedido es una posición nueva.
    // Devuelve {"orden":N}: la entidad lleva un LONGBLOB y no debe viajar en el JSON.
    // `orden` y `foto` van required=false a propósito: si faltaran, Spring lanzaría
    // MissingServletRequestPartException y el catch-all la respondería como 500.
    // Así el fallo sale como el 400 que corresponde, con su mensaje.
    @PostMapping(value = "/eventos/{id}/galeria", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR')")
    public ResponseEntity<?> subirFotoGaleria(@PathVariable Long id,
                                              @RequestParam(value = "orden", required = false) Integer orden,
                                              @RequestParam(value = "foto", required = false) MultipartFile foto,
                                              Authentication authentication) {
        if (orden == null || orden < 0 || orden >= MAX_FOTOS) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El orden debe estar entre 0 y " + (MAX_FOTOS - 1)));
        }
        if (foto == null || foto.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La foto es obligatoria"));
        }
        ResponseEntity<?> error = validarArchivo(foto);
        if (error != null) return error;

        Evento evento = eventoRepository.findById(id).orElse(null);
        if (evento == null) {
            return ResponseEntity.notFound().build();
        }
        if (!isAdmin(authentication) && !esDueno(evento, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo puedes subir fotos a tus propios eventos"));
        }
        var existente = fotoGaleriaRepository.findByEventoIdAndOrden(id, orden);
        if (existente.isEmpty() && fotoGaleriaRepository.countDistinctOrdenByEventoId(id) >= MAX_FOTOS) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Máximo " + MAX_FOTOS + " fotos por evento"));
        }
        FotoGaleria f = existente.orElseGet(FotoGaleria::new);
        f.setEvento(evento);
        f.setOrden(orden);
        try {
            f.setDatos(foto.getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la foto subida", e);
        }
        f.setContentType(foto.getContentType());
        fotoGaleriaRepository.save(f);
        return ResponseEntity.status(existente.isPresent() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(Map.of("orden", orden));
    }

    // FR-006. 404 si el evento no existe o si esa posición está libre.
    @DeleteMapping("/eventos/{id}/galeria/{orden}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR')")
    public ResponseEntity<?> borrarFotoGaleria(@PathVariable Long id,
                                               @PathVariable int orden,
                                               Authentication authentication) {
        Evento evento = eventoRepository.findById(id).orElse(null);
        if (evento == null) {
            return ResponseEntity.notFound().build();
        }
        if (!isAdmin(authentication) && !esDueno(evento, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo puedes borrar fotos de tus propios eventos"));
        }
        var foto = fotoGaleriaRepository.findByEventoIdAndOrden(id, orden);
        if (foto.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        fotoGaleriaRepository.delete(foto.get());
        return ResponseEntity.noContent().build();
    }

    // JPEG/PNG/WebP (los GIF no se admiten); el frontend ya redimensiona antes de subir,
    // el tope del backend es solo red de seguridad ante subidas directas a la API.
    private static final Set<String> TIPOS_CARTEL = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_CARTEL_BYTES = 2 * 1024 * 1024;

    private void aplicar(EventoDTO dto, MultipartFile file, MultipartFile fileHd, Evento evento) {
        evento.setEstablecimiento(dto.establecimiento());
        evento.setDireccion(dto.direccion());
        evento.setFecha(dto.fecha());
        evento.setNombre(dto.nombre());
        evento.setDescripcion(dto.descripcion());
        evento.setCartelUrl(dto.cartelUrl());
        evento.setHoraInicio(dto.horaInicio());
        evento.setHoraFin(dto.horaFin());
        // Sin fecha fin = un día (se guarda fecha para no tener dos fuentes de verdad)
        evento.setFechaFin(dto.fechaFin() != null ? dto.fechaFin() : dto.fecha());
        evento.setEtiquetas(resolverEtiquetas(dto));
        evento.setMapaEmbed(dto.mapaEmbed());
        evento.setRedesSociales(resolverRedesSociales(dto));
        evento.setTelefonoEvento(dto.telefonoEvento());
        evento.setUrlEvento(dto.urlEvento());
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

    // Punto 2: el DTO trae nombres libres (ya no hay enum) — nombre desconocido → 400
    private ResponseEntity<?> validarEtiquetas(EventoDTO dto) {
        List<String> desconocidas = etiquetasLimpias(dto.etiquetas()).stream()
                .filter(n -> etiquetaRepository.findByNombre(n).isEmpty()).toList();
        if (!desconocidas.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Etiquetas desconocidas: " + String.join(", ", desconocidas)));
        }
        return null;
    }

    // null/vacío = ["OTROS"] por defecto (punto 2). El orElseGet solo es red de
    // seguridad ante carreras entre validar y guardar — lo normal es que existan.
    private java.util.Set<Etiqueta> resolverEtiquetas(EventoDTO dto) {
        List<String> pedidas = etiquetasLimpias(dto.etiquetas());
        if (pedidas.isEmpty()) pedidas = List.of("OTROS");
        java.util.Set<Etiqueta> res = new java.util.HashSet<>();
        for (String n : pedidas) {
            res.add(etiquetaRepository.findByNombre(n)
                    .orElseGet(() -> etiquetaRepository.save(new Etiqueta(null, n))));
        }
        return res;
    }

    // 007 US6: panel del organizador (solo sus eventos, en cualquier estado) — 014: paginado
    @GetMapping("/eventos/mios")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public ResponseEntity<?> mios(@RequestParam(defaultValue = "0") int page,
                                  Authentication authentication) {
        Usuario organizador = usuarioActual(authentication);
        if (organizador == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario autenticado no encontrado"));
        }
        return ok(eventoRepository.findByCreadoPorOrderByIdAsc(organizador,
                org.springframework.data.domain.PageRequest.of(page, pageSize)));
    }

    // 007 US2: cola de moderación (revisiones + eliminaciones pendientes), solo admin
    @GetMapping("/eventos/pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventoResponseDTO>> pendientes() {
        return ok(eventoRepository.findByEstadoInOrderByIdAsc(
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
                    return ok(guardado);
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
                        return ok(eventoRepository.save(evento));
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
                    return ok(guardado);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 016 (Phase 4): null/vacío = sin redes. Repetir la misma red REEMPLAZA el valor
    // anterior (un evento no puede tener la misma red dos veces: unique evento_id+red).
    // Se ignora la que venga con red null/ausente (el enum inválido ya da 400 antes).
    private List<RedSocialEvento> resolverRedesSociales(EventoDTO dto) {
        if (dto.redesSociales() == null) return new ArrayList<>();
        java.util.LinkedHashMap<RedSocial, RedSocialEvento> porRed = new java.util.LinkedHashMap<>();
        for (RedSocialDTO r : dto.redesSociales()) {
            if (r == null || r.red() == null) continue;
            porRed.put(r.red(), new RedSocialEvento(r.red(), r.url()));
        }
        return new ArrayList<>(porRed.values());
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
        return ok(eventos);
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

    // 017 FR-001: ninguna respuesta sale con la entidad Evento (creadoPor traía el
    // Usuario completo: teléfono, apellidos, encargado, roles...). Las sobrecargas
    // resuelven en tiempo de compilación por el tipo estático de cada argumento.
    // null-safe: ResponseEntity.ok(null) era el comportamiento previo y debe seguir
    // siéndolo (un @MockitoBean sin stub devuelve null en el slice @WebMvcTest).
    private static ResponseEntity<EventoResponseDTO> ok(Evento evento) {
        return ResponseEntity.ok(evento == null ? null : EventoResponseDTO.fromEntity(evento));
    }

    private static ResponseEntity<org.springframework.data.domain.Page<EventoResponseDTO>> ok(
            org.springframework.data.domain.Page<Evento> pagina) {
        return ResponseEntity.ok(pagina == null ? null : pagina.map(EventoResponseDTO::fromEntity));
    }

    private static ResponseEntity<List<EventoResponseDTO>> ok(List<Evento> eventos) {
        return ResponseEntity.ok(eventos == null ? null
                : eventos.stream().map(EventoResponseDTO::fromEntity).toList());
    }

    // 007: helpers de rol y usuario autenticado
    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    // null-safe: un evento legacy puede no tener creador, y el email nunca es null aquí.
    private boolean esDueno(Evento evento, Authentication authentication) {
        return authentication != null
                && evento.getCreadoPor() != null
                && authentication.getName().equals(evento.getCreadoPor().getEmail());
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
            if (!magicBytesValidos(file)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El archivo no es una imagen válida"));
            }
        }
        return null;
    }

    // 017 FR-005: la firma real del fichero, no el Content-Type que declara el navegador
    // (renombrar un .exe a .jpg ya no basta). JPEG FF D8, PNG 89 50 4E 47, WebP RIFF…WEBP.
    private static boolean magicBytesValidos(MultipartFile file) {
        byte[] b;
        try {
            b = file.getBytes();
        } catch (IOException e) {
            return false;
        }
        if (b == null) return false;
        return switch (file.getContentType()) {
            case "image/jpeg" -> b.length >= 2 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8;
            case "image/png" -> b.length >= 4
                    && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            case "image/webp" -> b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            default -> false;
        };
    }
}
