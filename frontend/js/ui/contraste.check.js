// SPDX-License-Identifier: MIT

// Self-check de contraste WCAG 2.2 AA (022-accesibilidad-wcag-aa, T220).
// Se ejecuta con:
//     node js/ui/contraste.check.js
// Sin framework ni fixtures: lee la paleta real de css/variables.css y calcula
// el ratio de contraste (WCAG 2.1) de cada par de colores que la app pinta de
// verdad —texto sobre su fondo—, no de todos los pares posibles.
// Falla si un token se oscurece/clarorea hasta bajar de 4.5:1, que es exactamente
// como se rompió la paleta original (#ff6347 con texto blanco daba 2.95:1).
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

// ------------------------------------------------------------- paleta real
const CSS = new URL("../../css/variables.css", import.meta.url);
const fuente = readFileSync(CSS, "utf8");

const paleta = {};
for (const [, nombre, valor] of fuente.matchAll(/(--[\w-]+)\s*:\s*(#[0-9a-fA-F]{3,8})\s*;/g)) {
    paleta[nombre] = valor;
}
assert.ok(Object.keys(paleta).length > 0, "no se encontró ninguna variable en variables.css");

// --------------------------------------------------------- utilidad WCAG
function lineal(c) {
    c /= 255;
    return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
}

function luminancia(hex) {
    const n = parseInt(hex.slice(1), 16);
    const r = (n >> 16) & 255;
    const g = (n >> 8) & 255;
    const b = n & 255;
    return 0.2126 * lineal(r) + 0.7152 * lineal(g) + 0.0722 * lineal(b);
}

function ratio(a, b) {
    const [alto, bajo] = [luminancia(a), luminancia(b)].sort((x, y) => y - x);
    return (alto + 0.05) / (bajo + 0.05);
}

// ------------------------------------------------------------------ pares
// [color de texto, fondo, ratio mínimo, dónde se pinta]
// El mínimo es 4.5:1 (texto normal). Los pares con fondo fijo (no es token)
// se escriben como hex literal para poder mirarlos también.
const PARES = [
    ["--color-primary", "--color-surface", 4.5, "texto del tomate sobre blanco (enlaces del panel)"],
    ["--color-primary", "ffffff", 4.5, "blanco sobre cabecera, pie y botones"],
    ["--color-primary-hover", "--color-surface", 4.5, "blanco sobre el hover de botón / mapa"],
    ["--color-text", "--color-bg", 4.5, "texto normal sobre el fondo azul"],
    ["--color-text", "--color-surface", 4.5, "texto normal sobre las tarjetas"],
    ["--color-text", "--color-secondary", 4.5, "texto sobre los paneles salmón"],
    ["--color-text-muted", "--color-surface", 4.5, "texto apagado sobre blanco (placeholder)"],
    ["ffffff", "--color-text-muted", 4.5, "badge por defecto"],
    ["ffffff", "--color-estado-pendiente", 4.5, "badge pendiente"],
    ["ffffff", "--color-estado-aprobado", 4.5, "badge aprobado"],
    ["ffffff", "--color-estado-rechazado", 4.5, "badge rechazado"],
    ["ffffff", "--color-estado-eliminacion", 4.5, "badge pendiente de eliminación"],
    ["ffffff", "--color-rol-admin", 4.5, "badge rol admin"],
    ["ffffff", "--color-rol-organizador", 4.5, "badge rol organizador"],
    ["ffffff", "--color-rol-user", 4.5, "badge rol usuario"],
    ["--color-favorito", "--color-surface", 4.5, "estrella de favorito sobre la tarjeta"],
    // Fijos en eventos.css (no son variables): se listan igual para que alguien
    // que toque el aviso de revisión vea el ratio en este mismo check.
    ["663c00", "fff3cd", 4.5, "aviso de pendiente de revisión"],
    ["c62828", "ffffff", 3.0, "borde del slot de galería en modo borrar (no texto)"],
    ["--color-text", "--color-primary", 3.0, "contorno de foco sobre cabecera/pie (2.4.11)"],
];

const hex = nombre => (nombre.startsWith("--") ? paleta[nombre] : `#${nombre}`);

const fallos = [];
for (const [texto, fondo, minimo, donde] of PARES) {
    for (const nombre of [texto, fondo]) {
        if (nombre.startsWith("--")) {
            assert.ok(paleta[nombre], `falta la variable ${nombre} en variables.css`);
        }
    }
    const r = ratio(hex(texto), hex(fondo));
    if (r < minimo) {
        fallos.push(`${donde}: ${hex(texto)} sobre ${hex(fondo)} = ${r.toFixed(2)}:1 (mínimo ${minimo})`);
    }
}

assert.deepEqual(fallos, [], `contraste por debajo de WCAG AA:\n  ${fallos.join("\n  ")}`);

// ------------------------------------------------- reglas de foco presentes
// T221: si alguien borra el `:focus-visible` global, la navegación por teclado
// vuelve a depender del anillo por defecto del navegador (inconsistente).
for (const archivo of ["../../css/index.css", "../../css/eventos.css"]) {
    const css = readFileSync(new URL(archivo, import.meta.url), "utf8");
    assert.match(css, /:focus-visible\s*\{/, `${archivo} perdió la regla :focus-visible (T221)`);
    assert.match(css, /outline:\s*3px solid/, `${archivo}: el foco no dibuja outline (T221)`);
}

console.log(`contraste.check: ${PARES.length} pares de color en AA, foco visible en index.css y eventos.css`);
console.log("exit errors=0");
