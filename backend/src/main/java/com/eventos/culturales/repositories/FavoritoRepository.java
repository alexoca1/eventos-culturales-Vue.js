package com.eventos.culturales.repositories;

import com.eventos.culturales.entities.EstadoEvento;
import com.eventos.culturales.entities.Evento;
import com.eventos.culturales.entities.Favorito;
import com.eventos.culturales.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    Optional<Favorito> findByUsuarioAndEvento(Usuario usuario, Evento evento);

    List<Favorito> findByUsuarioAndEventoEstado(Usuario usuario, EstadoEvento estado);

    @Transactional
    void deleteByEvento(Evento evento);

    List<Favorito> findByEventoFechaAndEventoEstadoAndRecordatorioEnviadoFalse(
            LocalDate fecha, EstadoEvento estado, boolean recordatorioEnviado);
}
