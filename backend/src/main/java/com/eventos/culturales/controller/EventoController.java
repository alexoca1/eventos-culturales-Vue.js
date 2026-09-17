package com.eventos.culturales.controller;

import com.eventos.culturales.dto.EventoDTO;
import com.eventos.culturales.entities.Evento;
import com.eventos.culturales.repositories.EventoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class EventoController {

    private final EventoRepository eventoRepository;

    // Público (cadena 1 de SecurityConfig): con ?fecha= devuelve TODOS los de ese día, sin ella todos ordenados
    @GetMapping("/eventos")
    public ResponseEntity<List<Evento>> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        if (fecha != null) {
            return ResponseEntity.ok(eventoRepository.findByFechaOrderByIdAsc(fecha));
        }
        return ResponseEntity.ok(eventoRepository.findAllByOrderByFechaAscIdAsc());
    }

    // Público (cadena 1 de SecurityConfig)
    @GetMapping("/eventos/{id}")
    public ResponseEntity<Evento> findById(@PathVariable Long id) {
        return eventoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/eventos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createEvento(@Valid @RequestPart("evento") EventoDTO dto,
                                          @RequestPart(value = "file", required = false) MultipartFile file,
                                          @RequestPart(value = "fileHd", required = false) MultipartFile fileHd) {
        ResponseEntity<?> error = validar(dto, file);
        if (error != null) return error;
        error = validar(dto, fileHd);
        if (error != null) return error;
        Evento evento = new Evento();
        aplicar(dto, file, fileHd, evento);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoRepository.save(evento));
    }

    @PutMapping(value = "/eventos/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateEvento(@Valid @RequestPart("evento") EventoDTO dto,
                                          @RequestPart(value = "file", required = false) MultipartFile file,
                                          @RequestPart(value = "fileHd", required = false) MultipartFile fileHd,
                                          @PathVariable Long id) {
        ResponseEntity<?> error = validar(dto, file);
        if (error != null) return error;
        error = validar(dto, fileHd);
        if (error != null) return error;
        return eventoRepository.findById(id)
                .map(evento -> {
                    aplicar(dto, file, fileHd, evento);
                    return ResponseEntity.ok(eventoRepository.save(evento));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/eventos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> deleteEvento(@PathVariable Long id) {
        return eventoRepository.findById(id)
                .map(evento -> {
                    eventoRepository.deleteById(id);
                    return ResponseEntity.noContent().build();
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

    private ResponseEntity<?> validar(EventoDTO dto, MultipartFile file) {
        if (dto.mapaEmbed() != null && !dto.mapaEmbed().isBlank()
                && !dto.mapaEmbed().contains("google.com/maps/embed")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El mapa debe ser un iframe embed válido de Google Maps (google.com/maps/embed)"));
        }
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
