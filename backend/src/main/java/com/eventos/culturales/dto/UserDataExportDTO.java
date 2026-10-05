// SPDX-License-Identifier: MIT

package com.eventos.culturales.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

// 018 FR-002: portabilidad de datos (art. 20 RGPD). Todo lo que el sistema tiene
// del usuario, en JSON descargable. Nunca incluye el hash de la contraseña.
public record UserDataExportDTO(
        Long id,
        String email,
        String nombre,
        String apellidos,
        String telefono,
        String nombreOrganizacion,
        String encargadoNombre,
        String encargadoTelefono,
        String encargadoEmail,
        Set<String> roles,
        LocalDateTime fechaRegistro,
        List<EventoResponseDTO> eventosCreados,
        List<EventoResponseDTO> favoritos
) {}
