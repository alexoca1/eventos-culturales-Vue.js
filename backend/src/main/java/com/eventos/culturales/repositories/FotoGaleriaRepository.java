package com.eventos.culturales.repositories;

import com.eventos.culturales.entities.FotoGaleria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FotoGaleriaRepository extends JpaRepository<FotoGaleria, Long> {

    Optional<FotoGaleria> findByEventoIdAndOrden(Long eventoId, int orden);

    // Proyección: solo los órdenes ocupados, no las fotos (FR-007). Una query derivada
    // no puede proyectar una sola propiedad, de ahí el @Query.
    @Query("SELECT f.orden FROM FotoGaleria f WHERE f.evento.id = :eventoId ORDER BY f.orden")
    List<Integer> findOrdenesByEventoId(@Param("eventoId") Long eventoId);

    // Derived: count(distinct orden). "distinct" cuenta posiciones, no filas, que es
    // justo lo que pide el tope de 5 (FR-009).
    long countDistinctOrdenByEventoId(Long eventoId);
}
