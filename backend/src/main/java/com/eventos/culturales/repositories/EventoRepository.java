package com.eventos.culturales.repositories;

import com.eventos.culturales.entities.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {

    // Varios eventos pueden compartir fecha: devuelve TODOS los del día
    List<Evento> findByFechaOrderByIdAsc(LocalDate fecha);

    List<Evento> findAllByOrderByFechaAscIdAsc();
}
