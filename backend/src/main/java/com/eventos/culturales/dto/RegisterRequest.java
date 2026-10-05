// SPDX-License-Identifier: MIT

package com.eventos.culturales.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(@NotBlank(message = "El email es obligatorio")
                              @Email(message = "El email debe ser válido")
                              String email,

                              // 017 FR-003: 8+ con mayúscula, minúscula y número.
                              // (?=...) son lookaheads sobre la misma posición: la contraseña
                              // entera debe cumplir los tres, no uno por carácter.
                              @NotBlank(message = "La contraseña es obligatoria")
                              @Size(min = 8, max = 100,
                                      message = "La contraseña debe tener al menos 8 caracteres")
                              @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                                      message = "La contraseña debe contener mayúsculas, minúsculas y números")
                              String password,

                              @NotBlank(message = "El nombre es obligatorio")
                              String nombre,

                               @NotBlank(message = "Los apellidos son obligatorios")
                               String apellidos,

                               @NotBlank(message = "El teléfono es obligatorio")
                               String telefono) {}
