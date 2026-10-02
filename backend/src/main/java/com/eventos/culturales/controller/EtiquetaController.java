package com.eventos.culturales.controller;

import com.eventos.culturales.entities.Etiqueta;
import com.eventos.culturales.repositories.EtiquetaRepository;
import com.eventos.culturales.repositories.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Punto 2: catálogo de etiquetas gestionado por el admin.
// GET público (los filtros lo usan sin login); mutaciones solo ROLE_ADMIN.
@RestController
@RequiredArgsConstructor
public class EtiquetaController {

    private final EtiquetaRepository etiquetaRepository;
    private final EventoRepository eventoRepository;

    @GetMapping("/etiquetas")
    public ResponseEntity<List<Etiqueta>> listar() {
        return ResponseEntity.ok(etiquetaRepository.findAllByOrderByNombreAsc());
    }

    @PostMapping("/etiquetas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> crear(@RequestBody Map<String, String> body) {
        String nombre = body.get("nombre") == null ? "" : body.get("nombre").trim();
        if (nombre.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre de la etiqueta es obligatorio"));
        }
        if (etiquetaRepository.findByNombre(nombre).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ya existe una etiqueta con ese nombre"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(etiquetaRepository.save(new Etiqueta(null, nombre)));
    }

    @PutMapping("/etiquetas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> renombrar(@PathVariable Long id, @RequestBody Map<String, String> body) {
        var opt = etiquetaRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String nombre = body.get("nombre") == null ? "" : body.get("nombre").trim();
        if (nombre.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre de la etiqueta es obligatorio"));
        }
        var duplicada = etiquetaRepository.findByNombre(nombre);
        if (duplicada.isPresent() && !duplicada.get().getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ya existe una etiqueta con ese nombre"));
        }
        Etiqueta etiqueta = opt.get();
        etiqueta.setNombre(nombre);
        return ResponseEntity.ok(etiquetaRepository.save(etiqueta));
    }

    @DeleteMapping("/etiquetas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        var opt = etiquetaRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (eventoRepository.existsByEtiquetasId(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "No se puede eliminar: hay eventos con esta etiqueta"));
        }
        etiquetaRepository.delete(opt.get());
        return ResponseEntity.noContent().build();
    }
}
