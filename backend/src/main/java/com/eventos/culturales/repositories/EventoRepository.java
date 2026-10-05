// SPDX-License-Identifier: MIT

package com.eventos.culturales.repositories;

import com.eventos.culturales.entities.EstadoEvento;
import com.eventos.culturales.entities.Evento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {

    // 014: los listados devuelven Page (los 4 de fecha exacta siguen con List, ver abajo)
    Page<Evento> findAllByOrderByFechaAscIdAsc(Pageable pageable);

    // Punto 2: filtro por etiquetas con coincidencia ANY (DISTINCT evita duplicados
    // cuando un evento lleva varias de las pedidas).
    Page<Evento> findDistinctByEtiquetasNombreInOrderByIdAsc(java.util.Collection<String> nombres, Pageable pageable);

    // 007: cola de moderación (pendientes de revisión y de eliminación) — 014: SIN paginar
    List<Evento> findByEstadoInOrderByIdAsc(java.util.Collection<EstadoEvento> estados);

    // 007: panel del organizador (solo sus eventos, en cualquier estado) — 014: paginado
    Page<Evento> findByCreadoPorOrderByIdAsc(com.eventos.culturales.entities.Usuario creadoPor, Pageable pageable);

    // 018 FR-002: exportación RGPD (sin paginar: el histórico de un usuario es pequeño)
    List<Evento> findByCreadoPor(com.eventos.culturales.entities.Usuario creadoPor);

    // 006: un evento aparece en cada día de su rango [fecha, fechaFin]
    // (fechaFin null de filas antiguas = un día)
    // 014: SIN paginar (búsqueda por fecha exacta, suele ser pequeña)
    @Query("SELECT e FROM Evento e WHERE e.fecha <= :dia AND (e.fechaFin IS NULL OR e.fechaFin >= :dia) ORDER BY e.id")
    List<Evento> findVigentesEn(LocalDate dia);

    @Query("SELECT DISTINCT e FROM Evento e JOIN e.etiquetas t WHERE e.fecha <= :dia AND (e.fechaFin IS NULL OR e.fechaFin >= :dia) AND t.nombre IN :nombres ORDER BY e.id")
    List<Evento> findVigentesEnPorEtiquetas(LocalDate dia, java.util.Collection<String> nombres);

    // 007: variantes solo-APROBADO para el público (no admin) — 014: paginadas
    Page<Evento> findByEstadoOrderByFechaAscIdAsc(EstadoEvento estado, Pageable pageable);

    Page<Evento> findDistinctByEstadoAndEtiquetasNombreInOrderByIdAsc(EstadoEvento estado, java.util.Collection<String> nombres, Pageable pageable);

    // ¿Algún evento usa esta etiqueta? (DELETE de etiquetas la rechaza con 409 si sí)
    boolean existsByEtiquetasId(Long etiquetaId);

    @Query("SELECT e FROM Evento e WHERE e.estado = :estado AND e.fecha <= :dia AND (e.fechaFin IS NULL OR e.fechaFin >= :dia) ORDER BY e.id")
    List<Evento> findVigentesEnPorEstado(EstadoEvento estado, LocalDate dia);

    @Query("SELECT DISTINCT e FROM Evento e JOIN e.etiquetas t WHERE e.estado = :estado AND e.fecha <= :dia AND (e.fechaFin IS NULL OR e.fechaFin >= :dia) AND t.nombre IN :nombres ORDER BY e.id")
    List<Evento> findVigentesEnPorEstadoYEtiquetas(EstadoEvento estado, LocalDate dia, java.util.Collection<String> nombres);

    @Query(value = "SELECT categoria FROM eventos WHERE id = :id", nativeQuery = true)
    String categoriaLegacyDe(Long id);

    // 015 T150: eventos anteriores a la feature, que aún no tienen `nombre`.
    // Al añadir la columna como NOT NULL, MySQL rellena las filas viejas con ''
    // (no null), así que el filtro cubre los dos casos: con "IS NULL" a secas
    // el backfill no migraría ninguna fila y el nombre se perdería.
    @Query("SELECT e FROM Evento e WHERE e.nombre IS NULL OR TRIM(e.nombre) = ''")
    List<Evento> findSinNombre();

    // Punto 6: próximos (014: paginados; el countQuery con DISTINCT evita inflar totalElements)
    @Query("SELECT e FROM Evento e WHERE (e.fechaFin IS NULL OR e.fechaFin >= :hoy) ORDER BY e.fecha ASC, e.id ASC")
    Page<Evento> findFuturos(LocalDate hoy, Pageable pageable);

    @Query(value = "SELECT DISTINCT e FROM Evento e JOIN e.etiquetas t WHERE (e.fechaFin IS NULL OR e.fechaFin >= :hoy) AND t.nombre IN :nombres ORDER BY e.fecha ASC, e.id ASC",
            countQuery = "SELECT COUNT(DISTINCT e) FROM Evento e JOIN e.etiquetas t WHERE (e.fechaFin IS NULL OR e.fechaFin >= :hoy) AND t.nombre IN :nombres")
    Page<Evento> findFuturosPorEtiquetas(LocalDate hoy, java.util.Collection<String> nombres, Pageable pageable);

    @Query("SELECT e FROM Evento e WHERE e.estado = :estado AND (e.fechaFin IS NULL OR e.fechaFin >= :hoy) ORDER BY e.fecha ASC, e.id ASC")
    Page<Evento> findFuturosPorEstado(EstadoEvento estado, LocalDate hoy, Pageable pageable);

    @Query(value = "SELECT DISTINCT e FROM Evento e JOIN e.etiquetas t WHERE e.estado = :estado AND (e.fechaFin IS NULL OR e.fechaFin >= :hoy) AND t.nombre IN :nombres ORDER BY e.fecha ASC, e.id ASC",
            countQuery = "SELECT COUNT(DISTINCT e) FROM Evento e JOIN e.etiquetas t WHERE e.estado = :estado AND (e.fechaFin IS NULL OR e.fechaFin >= :hoy) AND t.nombre IN :nombres")
    Page<Evento> findFuturosPorEstadoYEtiquetas(EstadoEvento estado, LocalDate hoy, java.util.Collection<String> nombres, Pageable pageable);

    // 016: búsqueda por texto en nombre o establecimiento (LIKE, case-insensitive).
    // `q` llega YA escapado desde el controller (los comodines % y _ son literales).
    // Público: solo APROBADO.
    @Query("SELECT e FROM Evento e WHERE e.estado = :estado AND " +
           "(LOWER(e.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(e.establecimiento) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY e.fecha ASC, e.id ASC")
    Page<Evento> buscarPorTextoYEstado(String q, EstadoEvento estado, Pageable p);

    // 016: admin — los mismos criterios pero en CUALQUIER estado.
    @Query("SELECT e FROM Evento e WHERE " +
           "(LOWER(e.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(e.establecimiento) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY e.fecha ASC, e.id ASC")
    Page<Evento> buscarPorTexto(String q, Pageable p);
}
