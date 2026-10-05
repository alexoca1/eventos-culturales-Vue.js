// SPDX-License-Identifier: MIT

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

    // 018 FR-002: exportación de datos del usuario
    List<Favorito> findByUsuario(Usuario usuario);

    @Transactional
    void deleteByEvento(Evento evento);

    // 018 FR-001: supresión de cuenta — se borran los favoritos, no el usuario
    @Transactional
    void deleteByUsuario(Usuario usuario);

    List<Favorito> findByEventoFechaAndEventoEstadoAndRecordatorioEnviadoFalse(
            LocalDate fecha, EstadoEvento estado, boolean recordatorioEnviado);
}
