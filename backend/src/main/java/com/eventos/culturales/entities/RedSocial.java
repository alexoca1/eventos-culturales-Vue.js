package com.eventos.culturales.entities;

// 016 (Phase 4): redes sociales admitidas por evento. Se guardan por NOMBRE en BD
// (EnumType.STRING), nunca por ordinal, y el JSON del evento las serializa igual.
public enum RedSocial {
    FACEBOOK,
    INSTAGRAM,
    X,
    YOUTUBE,
    TIKTOK,
    LINKEDIN,
    WHATSAPP,
    TELEGRAM
}