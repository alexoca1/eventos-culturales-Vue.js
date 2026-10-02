package com.eventos.culturales.dto;

import com.eventos.culturales.entities.RedSocial;

// 016 (Phase 4): una red social del formulario. `red` es el enum (valor desconocido
// → 400, lo responde el handler de HttpMessageNotReadableException); `url` libre.
public record RedSocialDTO(
        RedSocial red,
        String url
) {}