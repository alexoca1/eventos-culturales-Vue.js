package com.eventos.culturales.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 016 (Phase 4): una red social concreta de un evento (red + url), pensada para
// @ElementCollection en Evento. La unicidad se fija en el @CollectionTable
// (evento_id, red): un evento no repite la misma red; el mapeo del controller
// deduplica con "último gana" para que duplicados en la petición se reemplacen.
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedSocialEvento {

    @Enumerated(EnumType.STRING)
    @Column(name = "red", length = 20, nullable = false)
    private RedSocial red;

    // URL de perfil/enlace de la red: varchar(500), no TEXT — es una URL corta.
    @Column(name = "url", length = 500, nullable = false)
    private String url;
}