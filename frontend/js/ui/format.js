// Helpers puros de formato (Fase 3 de 008-refactor-modular-esm).
// Copiados literalmente desde eventos.js, sin cambios de lógica; único cambio
// mecánico: this.formatDate()/this.textoEstado() -> llamadas directas del módulo.
// Todavía sin uso en ninguna página — solo existen, verificables con node --check.

export function formatDate(date) {
    if (!date) return '';
    const [year, month, day] = date.split('-');
    return `${day}/${month}/${year.slice(2)}`;
}

// "11/11/26 → 17/11/26" si dura varios días; "" si es de un día
export function formatRango(ev) {
    if (!ev.fechaFin || ev.fechaFin === ev.date) return '';
    return `${formatDate(ev.date)} → ${formatDate(ev.fechaFin)}`;
}

// "HH:mm - HH:mm" (+ aviso si cruza medianoche). NO recalcula el cruce:
// usa fechaFin tal cual lo trae la API (FR-003 frontend 002).
export function formatHorario(ev) {
    if (!ev.horaInicio || !ev.horaFin) return '';
    const rango = `${ev.horaInicio.slice(0, 5)} - ${ev.horaFin.slice(0, 5)}`;
    return ev.fechaFin && ev.fechaFin !== ev.date ? rango + " (día siguiente)" : rango;
}

// US5 004: texto legible del estado para el badge de "Mis eventos"
export function textoEstado(estado) {
    return {
        PENDIENTE_REVISION: "Pendiente de revisión",
        APROBADO: "Aprobado",
        RECHAZADO: "Rechazado",
        PENDIENTE_ELIMINACION: "Pendiente de eliminación"
    }[estado] || estado || "";
}

// US6 004: etiqueta del tipo de solicitud en la cola de moderación
export function tipoSolicitud(estado) {
    if (estado === "PENDIENTE_ELIMINACION") return "Solicitud de eliminación";
    if (estado === "PENDIENTE_REVISION") return "Creación/edición pendiente";
    return textoEstado(estado);
}

// US1 005: texto legible de un rol para el badge
export function textoRol(rol) {
    return { ROLE_ADMIN: "Admin", ROLE_ORGANIZADOR: "Organizador", ROLE_USER: "Usuario" }[rol] || rol;
}

// 019 (F192): texto legible del enlace de una red social del evento. `red` es el
// valor del enum del backend (RedSocial); fallback al propio valor si no se conoce.
export function textoRed(red) {
    return {
        FACEBOOK: "Facebook",
        INSTAGRAM: "Instagram",
        X: "X (Twitter)",
        YOUTUBE: "YouTube",
        TIKTOK: "TikTok",
        LINKEDIN: "LinkedIn",
        WHATSAPP: "WhatsApp",
        TELEGRAM: "Telegram"
    }[red] || red;
}

// 019 (F192): href de un teléfono para la tarjeta. El texto visible conserva el
// formato que tecleó el admin ("+34 926 123 456"); el URI solo admite dígitos y
// un '+' inicial (RFC 3966), así que se limpian espacios, guiones, puntos y
// paréntesis. Sin esto el href sale "tel:+34 926 123 456", que es inválido.
export function telHref(telefono) {
    return 'tel:' + String(telefono).replace(/[^\d+]/g, '');
}
