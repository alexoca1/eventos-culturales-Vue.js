package com.eventos.culturales.repositories;

import com.eventos.culturales.entities.CategoriaEvento;
import com.eventos.culturales.entities.EstadoEvento;
import com.eventos.culturales.entities.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {

    List<Evento> findAllByOrderByFechaAscIdAsc();

    List<Evento> findByCategoriaOrderByIdAsc(CategoriaEvento categoria);

    // 007: cola de moderación (pendientes de revisión y de eliminación)
    List<Evento> findByEstadoInOrderByIdAsc(java.util.Collection<EstadoEvento> estados);

    // 007: panel del organizador (solo sus eventos, en cualquier estado)
    List<Evento> findByCreadoPorOrderByIdAsc(com.eventos.culturales.entities.Usuario creadoPor);
    List<Evento> findByFechaAndCategoriaOrderByIdAsc(LocalDate fecha, CategoriaEvento categoria);

    // 006: un evento aparece en cada día de su rango [fecha, fechaFin]
    // (fechaFin null de filas antiguas = un día)
    @Query("SELECT e FROM Evento e WHERE e.fecha <= :dia AND (e.fechaFin IS NULL OR e.fechaFin >= :dia) ORDER BY e.id")
    List<Evento> findVigentesEn(LocalDate dia);

    @Query("SELECT e FROM Evento e WHERE e.fecha <= :dia AND (e.fechaFin IS NULL OR e.fechaFin >= :dia) AND e.categoria = :categoria ORDER BY e.id")
    List<Evento> findVigentesEnPorCategoria(LocalDate dia, CategoriaEvento categoria);

    // 007: variantes solo-APROBADO para el público (no admin)
    List<Evento> findByEstadoOrderByFechaAscIdAsc(EstadoEvento estado);

    List<Evento> findByEstadoAndCategoriaOrderByIdAsc(EstadoEvento estado, CategoriaEvento categoria);

    @Query("SELECT e FROM Evento e WHERE e.estado = :estado AND e.fecha <= :dia AND (e.fechaFin IS NULL OR e.fechaFin >= :dia) ORDER BY e.id")
    List<Evento> findVigentesEnPorEstado(EstadoEvento estado, LocalDate dia);

    @Query("SELECT e FROM Evento e WHERE e.estado = :estado AND e.fecha <= :dia AND (e.fechaFin IS NULL OR e.fechaFin >= :dia) AND e.categoria = :categoria ORDER BY e.id")
    List<Evento> findVigentesEnPorEstadoYCategoria(EstadoEvento estado, LocalDate dia, CategoriaEvento categoria);
}
