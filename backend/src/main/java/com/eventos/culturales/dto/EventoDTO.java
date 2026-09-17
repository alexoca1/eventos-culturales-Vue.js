package com.eventos.culturales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

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

        String mapaEmbed
) {}
