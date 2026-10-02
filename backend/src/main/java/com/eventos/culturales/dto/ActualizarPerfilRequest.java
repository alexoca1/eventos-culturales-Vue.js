package com.eventos.culturales.dto;

// Punto 4: edición del propio perfil (nunca roles/enabled: eso sigue siendo solo-admin).
public record ActualizarPerfilRequest(
        String nombre,
        String apellidos,
        String telefono,
        String nombreOrganizacion,
        String encargadoNombre,
        String encargadoTelefono,
        String encargadoEmail
) {}
