package com.eventos.culturales.dto;

import com.eventos.culturales.entities.CategoriaEvento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalTime;

public record EventoDTO(
        @NotBlank(message = "El establecimiento es obligatorio")
        String establecimiento,

        @NotBlank(message = "La dirección es obligatoria")
        String direccion,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotBlank(message = "La descripción es obligatoria")
        String descripcion,

        String cartelUrl,

        LocalTime horaInicio,

        LocalTime horaFin,

        // Opcional: null o ausente = un solo día (se normaliza a fecha al guardar)
        LocalDate fechaFin,

        CategoriaEvento categoria,

        // Solo un <iframe> de Google Maps con atributos permitidos (o vacío = sin mapa).
        // Cierra el XSS almacenado que permitía el antiguo .contains("google.com/maps/embed").
        @Pattern(regexp = "^(\\s*|<iframe(?=[^>]*\\ssrc=\"https://www\\.google\\.com/maps/embed[^\"]*\")(\\s+(src=\"https://www\\.google\\.com/maps/embed[^\"]*\"|width=\"\\d+\"|height=\"\\d+\"|style=\"[^\"]*\"|allowfullscreen(=\"[^\"]*\")?|loading=\"[A-Za-z]+\"|referrerpolicy=\"[^\"]*\"))+\\s*></iframe>)$",
                message = "El mapa debe ser un iframe embed válido de Google Maps")
        String mapaEmbed
) {}
