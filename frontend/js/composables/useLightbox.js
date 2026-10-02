// useLightbox (F091 de 008-refactor-modular-esm).
// Cartel HD en overlay (openLightbox, closeLightbox + listener global de Escape,
// hoy en mounted()). Misma firma y comportamiento que en eventos.js.
// Sin uso en páginas todavía (Fase 5).
import { ref } from '../../lib/vue.esm-browser.js';
import { API } from '../api/http.js';

let instance = null;
export function useLightbox() {
    if (instance) return instance;

    const lightboxSrc = ref("");
    const lightboxAlt = ref("cartel ampliado");

    // Clic en el cartel: abre la versión HD en el lightbox.
    // 019: las miniaturas de galería ya traen su URL completa (galeriaUrl), así que
    // se acepta id o URL. Se distingue por el esquema: un id nunca empieza por http.
    function openLightbox(idOUrl, alt) {
        lightboxSrc.value = /^https?:\/\//i.test(idOUrl)
            ? idOUrl
            : `${API}/eventos/${idOUrl}/cartel-hd`;
        lightboxAlt.value = alt || "cartel ampliado";
    }

    function closeLightbox() {
        lightboxSrc.value = "";
    }

    // Un cartel sin bytes en BD da 404 en /cartel-hd (eventos creados por API sin
    // poster): en vez de dejar el icono roto se muestra el banner de portada.
    function onLightboxError() {
        if (lightboxSrc.value && lightboxSrc.value !== "../img/evento_portada.jpg") {
            lightboxSrc.value = "../img/evento_portada.jpg";
        }
    }

    window.addEventListener("keydown", (e) => { if (e.key === "Escape") closeLightbox(); });

    instance = {
        lightboxSrc, lightboxAlt,
        openLightbox, closeLightbox, onLightboxError
    };
    return instance;
}
