# Implementation Plan: Etiquetas de Eventos — Eventos Culturales Puertollano

**Branch**: `005-categorias-eventos` | **Date**: 2026-09-18 (reescrito el 2026-09-24, punto 2: etiquetas múltiples con ruptura limpia) | **Spec**: `specs/005-categorias-eventos/spec.md`

> Reescritura retrospectiva: el diseño original (enum `CategoriaEvento`, ya eliminado) queda superado por el real. Fuente de verdad: `spec.md` + `tasks.md` (fases 4-6) de esta carpeta.

## Summary
Entidad `Etiqueta` (catálogo gestionado por el admin) + `Evento.etiquetas` `@ManyToMany` EAGER (join `evento_etiquetas`) + `EtiquetaController` (GET público, mutaciones admin) + `GET /eventos?etiquetas=a,b` con coincidencia ANY + migración de la antigua `categoria` en `DataInitializer`.

## Technical Context
**Language/Version**: Java 21, sin dependencias nuevas.
**Storage**: tablas `etiquetas` (nombre único) y `evento_etiquetas`; la columna legacy `categoria` se deja de mapear (ddl-auto=update no la borra, se ignora).
**Testing**: JUnit 5 + MockMvc, mismo estilo que features previas (slice `@WebMvcTest` + `@Import` de seguridad).

## Constitution Check
- [x] III. Sin over-engineering: sin capa Service (el controller habla directo con los dos repositorios, como el CRUD de eventos); sin referencia inversa `Etiqueta→Evento` (evita recursión JSON sin DTOs nuevos).
- [x] IV. Tests obligatorios: `EtiquetaControllerTest` (13) + reescritura de los de categoría (tasks.md T131-T135).
- [x] V. Seguridad explícita: `GET /etiquetas` en la cadena 1; mutaciones con `@PreAuthorize("hasRole('ADMIN')")`.

## Project Structure

### Documentation (this feature)
```text
specs/005-categorias-eventos/
├── plan.md
└── tasks.md
```

### Source Code
```text
backend/.../entities/Etiqueta.java            # nuevo (id, nombre único, sin backref)
backend/.../repositories/EtiquetaRepository.java  # nuevo (findByNombre, findAllByOrderByNombreAsc)
backend/.../entities/Evento.java              # - categoria, + etiquetas @ManyToMany EAGER
backend/.../dto/EventoDTO.java                # categoria -> etiquetas: List<String>
backend/.../repositories/EventoRepository.java  # queries ANY con DISTINCT + existsByEtiquetasId + categoriaLegacyDe (nativa, solo migración)
backend/.../controller/EventoController.java  # ?etiquetas= ANY, validarEtiquetas (400) + resolverEtiquetas (defecto OTROS)
backend/.../controller/EtiquetaController.java  # nuevo: GET público + POST/PUT/DELETE admin (409/404)
backend/.../config/SecurityConfig.java        # GET /etiquetas a la cadena 1
backend/.../config/DataInitializer.java       # seed de las 7 + migración legacy en el backfill
backend/.../controller/EtiquetaControllerTest.java  # nuevo (13 tests)
docs/api-contract.md                           # modelo etiquetas, ?etiquetas=, CRUD /etiquetas
```

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| Ninguna | — | — |
