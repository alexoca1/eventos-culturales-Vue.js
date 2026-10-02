// Helper puro de mapa (Fase 3 de 008-refactor-modular-esm).
// Copiado literalmente desde eventos.js, sin cambios de lógica.
// Todavía sin uso en ninguna página — solo existe, verificable con node --check.

export function processMap(iframe) {
    if (!iframe) return '';
    if (iframe.includes('google.com/maps/embed')) {
        return iframe.replace(/width="[^"]*"/i, 'width="400"')
            .replace(/height="[^"]*"/i, 'height="300"');
    }
    return iframe;
}
