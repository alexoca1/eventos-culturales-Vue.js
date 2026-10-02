package com.eventos.culturales.dto;

import java.util.Set;

public record ActualizarUsuarioRequest(
        Set<String> roles,
        Boolean enabled,
        // Punto 4: datos de organización (obligatorios si el rol final es ORGANIZADOR)
        String nombreOrganizacion,
        String encargadoNombre,
        String encargadoTelefono,
        String encargadoEmail
) {}
