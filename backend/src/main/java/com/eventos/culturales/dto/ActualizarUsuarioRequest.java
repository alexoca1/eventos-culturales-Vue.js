package com.eventos.culturales.dto;

import java.util.Set;

public record ActualizarUsuarioRequest(
        Set<String> roles,
        Boolean enabled
) {}
