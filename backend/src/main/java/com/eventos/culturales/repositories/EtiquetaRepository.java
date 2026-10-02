package com.eventos.culturales.repositories;

import com.eventos.culturales.entities.Etiqueta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EtiquetaRepository extends JpaRepository<Etiqueta, Long> {

    Optional<Etiqueta> findByNombre(String nombre);

    java.util.List<Etiqueta> findAllByOrderByNombreAsc();
}
