// SPDX-License-Identifier: MIT

package com.eventos.culturales.dto;

import com.eventos.culturales.entities.EstadoEvento;
import com.eventos.culturales.entities.Etiqueta;
import com.eventos.culturales.entities.Evento;
import com.eventos.culturales.entities.RedSocialEvento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

// 017 FR-001: la entidad Evento no sale en JSON (viajaba con el Usuario completo de
// creadoPor). Solo los campos que consume el frontend (ver frontend/js/api/eventos.js).
// Sin `cartel`/`cartelHd`/`fotos`: son LONGBLOB y ya iban con @JsonIgnore en la entidad.
public record EventoResponseDTO(
        Long id,
        String establecimiento,
        String direccion,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        LocalDate fechaFin,
        String nombre,
        String descripcion,
        String cartelUrl,
        EstadoEvento estado,
        CreadoPorResponseDTO creadoPor,
        String motivoRechazo,
        String mapaEmbed,
        Set<Etiqueta> etiquetas,
        List<RedSocialEvento> redesSociales,
        String telefonoEvento,
        String urlEvento,
        LocalDateTime fechaAlta,
        LocalDateTime fechaModificacion
) {
    public static EventoResponseDTO fromEntity(Evento e) {
        if (e == null) return null;
        return new EventoResponseDTO(
                e.getId(),
                e.getEstablecimiento(),
                e.getDireccion(),
                e.getFecha(),
                e.getHoraInicio(),
                e.getHoraFin(),
                e.getFechaFin(),
                e.getNombre(),
                e.getDescripcion(),
                e.getCartelUrl(),
                e.getEstado(),
                CreadoPorResponseDTO.fromEntity(e.getCreadoPor()),
                e.getMotivoRechazo(),
                e.getMapaEmbed(),
                e.getEtiquetas(),
                e.getRedesSociales(),
                e.getTelefonoEvento(),
                e.getUrlEvento(),
                e.getFechaAlta(),
                e.getFechaModificacion()
        );
    }
}
