// Subida y redimensionado de carteles (Fase 3 de 008-refactor-modular-esm).
// Copiados literalmente desde eventos.js, sin cambios de lógica; único cambio mecánico:
// handleFileUpload recibía el estado vía `this` (método del componente) y ahora lo recibe
// vía `state` (el objeto reactivo del composable: usa state.file, state.fileHd, state.inputPoster).
//(handleFileUpload) lo usan las vistas admin y organizador a través de admin.js y
// organizador.js.
// Redimensiona en el navegador a WebP: display 400px + HD tope 1600px.
// El usuario sube cualquier foto (aunque pese 12 MB) y viajan ~50KB + ~300KB.
// Solo se rechaza por tipo (los GIF no se admiten); por tamaño, nunca: webify() baja
// calidad y resolución hasta que el WebP cabe en MAX_BYTES_IMAGEN.
// Las dos conversiones del cartel (display + HD) pueden seguir en vuelo cuando el usuario
// ya ha pulsado guardar. Sin esperarlas, el POST sale con `state.file` todavía a null: al
// editar, el backend conserva el cartel viejo y la imagen recién elegida se pierde EN
// SILENCIO (el resto del evento sí se guarda, así que la app parece haber hecho su trabajo).
// Es el mismo problema que resolvió `procesandoGaleria` en el composable; aquí el set vive
// en el módulo porque el handler se consume desde la plantilla (@change) sin pasar por él.
const procesandoCartel = new Set();

export async function esperarCompresionCartel() {
    while (procesandoCartel.size) await Promise.all([...procesandoCartel]);
}

export function handleFileUpload(event, state) {
    const file = event.target.files[0];
    if (!file) return;
    const validImageTypes = ['image/jpeg', 'image/png', 'image/webp'];
    if (!validImageTypes.includes(file.type)) {
        alert("Por favor, selecciona una imagen válida (JPEG, PNG o WebP). Los GIF no se admiten.");
        return;
    }
    const conversion = new Promise((hecho) => {
        const img = new Image();
        img.onload = () => {
            URL.revokeObjectURL(img.src);
            const display = new Promise((listo) => webify(img, 400, file.name, (f) => {
                state.file = f;
                if (f) {
                    const url = URL.createObjectURL(f);
                    state.inputPoster = `<img src="${url}" alt="evento imagen" style="max-width: 400px; max-height: 400px;">`;
                }
                listo();
            }));
            const hd = new Promise((listo) => webify(img, 1600, file.name, (f) => {
                state.fileHd = f;
                listo();
            }));
            // El guardado solo necesita al display; el HD puede seguir detrás, pero se espera
            // también para no subir un HD a medio codificar.
            Promise.all([display, hd]).then(hecho, hecho);
        };
        img.onerror = () => hecho();   // fichero corrupto: no se bloquea el guardado
        img.src = URL.createObjectURL(file);
    });
    procesandoCartel.add(conversion);
    conversion.then(() => procesandoCartel.delete(conversion));
}

// Presupuesto por imagen. El INSERT del blob viaja en un paquete MariaDB de ~2x los bytes
// de la foto, así que con el max_allowed_packet por defecto (1 MB) cualquier foto de
// >= 512 KB revienta con PacketTooBigException (500 "Error interno"). 480 KB deja margen.
export const MAX_BYTES_IMAGEN = 480 * 1024;

// De más calidad a menos: la primera combinación que cabe en el presupuesto gana. Si el
// navegador no puede codificar (toBlob a null) se pasa al siguiente intento.
const INTENTOS = [
    { escala: 1, calidad: 0.85 },
    { escala: 1, calidad: 0.7 },
    { escala: 0.8, calidad: 0.7 },
    { escala: 0.8, calidad: 0.55 },
    { escala: 0.65, calidad: 0.55 },
];

export function webify(img, maxLado, nombre, listo) {
    const base = Math.min(1, maxLado / Math.max(img.naturalWidth, img.naturalHeight));
    const nombreWebp = nombre.replace(/\.[^.]+$/, "") + ".webp";
    let i = 0;
    let menor = null;
    (function intentar() {
        // ponytail: si ninguna combinación cabe (ruido extremo a 0.65x) se devuelve la
        // menor, que puede quedar sobre el presupuesto y rebotar en el backend. Subir el
        // presupuesto de verdad es subir max_allowed_packet de MariaDB, no más iteraciones.
        if (i >= INTENTOS.length) return listo(menor);
        const { escala, calidad } = INTENTOS[i++];
        const s = base * escala;
        const canvas = document.createElement('canvas');
        canvas.width = Math.max(1, Math.round(img.naturalWidth * s));
        canvas.height = Math.max(1, Math.round(img.naturalHeight * s));
        canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height);
        canvas.toBlob((blob) => {
            if (blob) {
                if (!menor || blob.size < menor.size) menor = blob;
                if (blob.size <= MAX_BYTES_IMAGEN) {
                    return listo(new File([blob], nombreWebp, { type: "image/webp" }));
                }
            }
            intentar();
        }, 'image/webp', calidad);
    })();
}
