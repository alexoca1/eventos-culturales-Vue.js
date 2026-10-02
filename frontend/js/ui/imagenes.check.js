// Self-check de la compresión de imágenes antes de subirlas (webify). Se ejecuta con:
//     node js/ui/imagenes.check.js
// Sin framework ni navegador: se stubea document/File y se comprueba la ESCALERA de
// calidad y resolución contra MAX_BYTES_IMAGEN. Es el guardarraíl del 500 "Error interno
// del servidor": el INSERT del LONGBLOB va en un paquete MariaDB de ~2x los bytes de la
// foto, así que una foto >= 512 KB revienta con PacketTooBigException.
import assert from "node:assert/strict";

// ------------------------------------------------------------- shim navegador
// El canvas falso devuelve un tamaño proporcional a píxeles x calidad, que es justo lo
// que decide la escalera: una foto enorme no cabe ni bajando calidad.
let pasos = [];
let emitidos = [];
let soloGrande = false;   // simula un encoder que nunca baja del mínimo
let toBlobFalla = false;
globalThis.document = {
    createElement() {
        const canvas = { width: 0, height: 0, getContext: () => ({ drawImage() {} }) };
        canvas.toBlob = (cb, _tipo, calidad) => {
            pasos.push({ ancho: canvas.width, alto: canvas.height, calidad });
            if (toBlobFalla) return cb(null);
            const natural = Math.round(canvas.width * canvas.height * calidad / 3);
            // con soloGrande los tamaños BAJAN por intento, para que "la menor" no sea "la primera"
            const size = soloGrande ? 3 * 1024 * 1024 - pasos.length * 200 * 1024 : natural;
            emitidos.push(size);
            cb({ size });
        };
        return canvas;
    }
};
globalThis.File = class {
    constructor(partes, nombre, opciones) {
        this.size = partes[0].size;
        this.name = nombre;
        this.type = opciones.type;
    }
};

const { webify, handleFileUpload, esperarCompresionCartel, MAX_BYTES_IMAGEN } = await import("./imagenes.js");

// Convierte el callback de webify en promesa, que es como lo consume la app.
const comprimir = (imagen, maxLado = 1600) => new Promise((res) => webify(imagen, maxLado, "foto.jpg", res));
const foto = (w, h) => ({ naturalWidth: w, naturalHeight: h });
const reiniciar = () => { pasos = []; emitidos = []; };

// 1. Foto que ya cabe: se queda con la máxima calidad y no se toca nada más.
reiniciar();
const comoda = await comprimir(foto(600, 400));
assert.ok(comoda, "una foto pequeña debe devolverse comprimida");
assert.equal(comoda.type, "image/webp", "el resultado es WebP");
assert.equal(comoda.name, "foto.webp", "se renombra a .webp");
assert.ok(comoda.size <= MAX_BYTES_IMAGEN, "el WebP devuelto cabe en el presupuesto");
assert.equal(pasos.length, 1, "si la primera combinación cabe, no hay que probar más");
assert.equal(pasos[0].calidad, 0.85, "una foto cómoda se queda en la máxima calidad");

// 2. Foto grande: baja calidad (y si no llega, resolución) hasta entrar en el presupuesto.
reiniciar();
const grande = await comprimir(foto(3000, 3000));
assert.ok(grande, "una foto grande debe devolverse comprimida, no null");
assert.ok(grande.size <= MAX_BYTES_IMAGEN,
    `una foto de 3000x3000 debe caber en ${MAX_BYTES_IMAGEN} bytes (salió ${grande.size})`);
assert.ok(pasos.length > 1, "una foto que no cabe a la primera tiene que recorrer la escalera");
assert.ok(pasos.at(-1).calidad < 0.85, "la escalera tiene que bajar la calidad para entrar");
assert.ok(pasos.at(-1).ancho <= 1600, "nunca se sube de maxLado");

// 3. El presupuesto manda siempre, también con geometría rara (panorámica y retrato).
reiniciar();
for (const [w, h] of [[6000, 900], [900, 6000], [2400, 2400]]) {
    const r = await comprimir(foto(w, h));
    assert.ok(r.size <= MAX_BYTES_IMAGEN, `${w}x${h} debe caber en el presupuesto (salió ${r.size})`);
}

// 4. Si nada cabe (encoder que no respeta la calidad): se devuelve la MENOR, nunca null.
//    Si devolviera null, la app subiría el original de 6 MB y el 500 volvería.
//    Los tamaños bajan por intento para distinguir "la menor" de "la primera".
reiniciar();
soloGrande = true;
const imposible = await comprimir(foto(3000, 3000));
soloGrande = false;
assert.ok(imposible, "cuando nada cabe se devuelve la mejor opción, no null");
assert.ok(Math.min(...emitidos) < emitidos[0], "el caso tiene que distinguir la menor de la primera");
assert.equal(imposible.size, Math.min(...emitidos),
    "el resultado es el menor de los intentos, no el primero");

// 5. Si el navegador no codifica (toBlob a null), se salta al siguiente intento.
reiniciar();
toBlobFalla = true;
const sinCanvas = await comprimir(foto(1200, 900));
toBlobFalla = false;
assert.equal(sinCanvas, null, "si todos los intentos fallan se avisa con null, no con un File roto");
assert.equal(pasos.length, 5, "se prueban los 5 intentos de la escalera antes de rendirse");

// 6. El CARTEL: sus dos conversiones (display + HD) quedan registradas para que quien
//    guarda pueda esperarlas. Regresión de la pérdida silenciosa: al elegir un cartel
//    grande y pulsar guardar sin esperar, el POST salía con file=null, el backend
//    conservaba el cartel anterior y la foto elegida se perdía sin ningún aviso (en modo
//    crear, en cambio, salía un "cartel obligatorio" falso). El Image falso dispara onload
//    en el siguiente tick, que es justo la ventana en la que ocurría.
globalThis.Image = class {
    constructor() { this.naturalWidth = 3000; this.naturalHeight = 2200; }
    set src(v) { setTimeout(() => this.onload && this.onload(), 0); }
};
// Ojo: se añaden métodos a la clase URL real, no se sustituye (este check usa new URL).
globalThis.URL.createObjectURL = () => "blob:check";
globalThis.URL.revokeObjectURL = () => {};
globalThis.alert = () => {};

reiniciar();
const state = { file: null, fileHd: null, inputPoster: "" };
const cartelPesado = new File([{ size: 5_000_000 }], "cartel.jpg", { type: "image/jpeg" });
handleFileUpload({ target: { files: [cartelPesado] } }, state);
assert.equal(state.file, null, "al elegir el archivo todavía no hay nada comprimido");
await esperarCompresionCartel();
assert.ok(state.file, "esperar debe dejar el cartel display comprimido y listo para subir");
assert.equal(state.file.type, "image/webp", "el cartel se sube en WebP");
assert.ok(state.file.size <= MAX_BYTES_IMAGEN,
    `el cartel display debe caber en el presupuesto (salió ${state.file?.size})`);
assert.ok(state.fileHd, "el HD también queda comprimido");
assert.ok(state.fileHd.size <= MAX_BYTES_IMAGEN,
    `el cartel HD debe caber en el presupuesto (salió ${state.fileHd?.size})`);
assert.ok(state.inputPoster.includes("blob:"), "la previsualización apunta al archivo comprimido");

// 7. El guardado tiene que USAR esa espera, y antes de validar: validarObligatorios() mira
//    file.value para exigir el cartel al crear, así que esperar después daría el error
//    "cartel obligatorio" en un evento al que se le acaba de elegir la imagen. El composable
//    no se importa aquí (arrastra Vue y la API), así que se comprueba sobre el fuente, igual
//    que los otros checks hacen con el HTML y el CSS.
const fuente = await (await import("node:fs/promises")).readFile(
    new URL("../composables/useEventoForm.js", import.meta.url), "utf8");
const esperaCartel = fuente.indexOf("await esperarCompresionCartel()");
assert.ok(esperaCartel !== -1,
    "useEventoForm.js debe esperar a esperarCompresionCartel(): si no, el cartel se pierde al guardar");
// La llamada, no la definición: validarObligatorios() se declara antes que submitForm.
const submitForm = fuente.indexOf("async function submitForm()");
assert.ok(submitForm !== -1, "useEventoForm.js debe tener submitForm()");
assert.ok(esperaCartel < fuente.indexOf("validarObligatorios()", submitForm),
    "la espera del cartel va ANTES de validarObligatorios(), que exige file.value al crear");
assert.ok(fuente.includes("await esperarCompresionGaleria()"),
    "las fotos de galería también se esperan antes del POST");

console.log(
    `OK  webify respeta el presupuesto de ${MAX_BYTES_IMAGEN} bytes (paquete MariaDB ~2x), ` +
    `escalera de 5 intentos con calidad y resolución, devuelve la menor si no cabe, y tanto ` +
    `el cartel como la galería se esperan antes de validar y guardar`
);
