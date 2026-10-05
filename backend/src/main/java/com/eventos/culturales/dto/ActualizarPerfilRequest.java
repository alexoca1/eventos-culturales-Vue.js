package com.eventos.culturales.dto;

import jakarta.validation.constraints.Email;

// Punto 4: edición del propio perfil (nunca roles/enabled: eso sigue siendo solo-admin).
// 023: el email pasa a ser editable, pero es el `subject` del JWT, así que quien lo
// cambie tiene que renovar el token o deja de estar identificado (ver usePerfil.js).
// Sin @NotBlank: null significa "no lo toco", igual que en el resto de campos. Un
// email en blanco o con formato inválido lo corta @Email con 400.
public record ActualizarPerfilRequest(
        @Email(message = "El correo debe ser válido")
        String email,

        String nombre,
        String apellidos,
        String telefono,
        String nombreOrganizacion,
        String encargadoNombre,
        String encargadoTelefono,
        String encargadoEmail
) {}