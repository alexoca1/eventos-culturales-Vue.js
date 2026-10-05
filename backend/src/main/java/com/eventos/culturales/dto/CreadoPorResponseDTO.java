// SPDX-License-Identifier: MIT

package com.eventos.culturales.dto;

import com.eventos.culturales.entities.Usuario;

// 017 FR-001: minimización de datos. GET /eventos no debe filtrar el Usuario completo
// (teléfono, apellidos, encargado, roles...), solo lo que la web necesita mostrar.
public record CreadoPorResponseDTO(
        Long id,
        String email,
        String nombreOrganizacion
) {
    public static CreadoPorResponseDTO fromEntity(Usuario u) {
        if (u == null) return null;
        return new CreadoPorResponseDTO(
                u.getId(),
                u.getEmail(),
                u.getNombreOrganizacion()
        );
    }
}
