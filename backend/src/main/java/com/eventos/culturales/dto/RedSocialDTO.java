// SPDX-License-Identifier: MIT

package com.eventos.culturales.dto;

import com.eventos.culturales.entities.RedSocial;
import jakarta.validation.constraints.Pattern;

// 016 (Phase 4): una red social del formulario. `red` es el enum (valor desconocido
// → 400, lo responde el handler de HttpMessageNotReadableException); `url` libre.
// 017 FR-002: la url solo admite http(s) (bloquea javascript:).
public record RedSocialDTO(
        RedSocial red,

        @Pattern(regexp = "^(https?://.*)?$",
                message = "La URL de la red social debe comenzar por http:// o https://")
        String url
) {}