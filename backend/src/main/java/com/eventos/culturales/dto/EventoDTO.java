// SPDX-License-Identifier: MIT

package com.eventos.culturales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EventoDTO(
        @NotBlank(message = "El establecimiento es obligatorio")
        String establecimiento,

        @NotBlank(message = "La dirección es obligatoria")
        String direccion,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        // 015 US1: nombre del evento (lo que antes venía en `descripcion`).
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        // 015 US1: opcional. Sigue en el record; ya no es obligatorio.
        String descripcion,

        // 017 FR-002: solo http(s) — bloquea javascript:, data:, etc. (XSS al abrir el enlace)
        @Pattern(regexp = "^(https?://.*)?$",
                message = "La URL del cartel debe comenzar por http:// o https://")
        String cartelUrl,

        LocalTime horaInicio,

        LocalTime horaFin,

        // Opcional: null o ausente = un solo día (se normaliza a fecha al guardar)
        LocalDate fechaFin,

        // Etiquetas múltiples (punto 2): null/vacío = ["OTROS"] por defecto.
        // Nombre desconocido → 400 (lo valida el controller, no el enum).
        List<String> etiquetas,

        // Solo un <iframe> de Google Maps con atributos permitidos (o vacío = sin mapa).
        // Cierra el XSS almacenado que permitía el antiguo .contains("google.com/maps/embed").
        @Pattern(regexp = "^(\\s*|<iframe(?=[^>]*\\ssrc=\"https://www\\.google\\.com/maps/embed[^\"]*\")(\\s+(src=\"https://www\\.google\\.com/maps/embed[^\"]*\"|width=\"\\d+\"|height=\"\\d+\"|style=\"[^\"]*\"|allowfullscreen(=\"[^\"]*\")?|loading=\"[A-Za-z]+\"|referrerpolicy=\"[^\"]*\"))+\\s*></iframe>)$",
                message = "El mapa debe ser un iframe embed válido de Google Maps")
        String mapaEmbed,

        // 016 (Phase 4): redes sociales y contacto, todos opcionales.
        // redesSociales: List<RedSocialDTO>; repetir la misma red reemplaza (último gana).
        // Valores desconocidos del enum → 400 (HttpMessageNotReadableException).
        // 017 FR-002: el @Valid de tipo cascada valida la url de cada red (solo http(s)).
        List<@jakarta.validation.Valid RedSocialDTO> redesSociales,

        String telefonoEvento,

        // 017 FR-002: solo http(s). Vacío/nulo sigue permitido (campo opcional).
        @Pattern(regexp = "^(https?://.*)?$",
                message = "La URL del evento debe comenzar por http:// o https://")
        String urlEvento
) {}
