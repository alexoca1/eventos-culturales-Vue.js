// Self-check de la vista persistente (019). Se ejecuta con:
//     node js/ui/vistas.check.js
// Sin framework ni fixtures. Tres partes:
//   1. invariantes del mapa de datos (vistas.js);
//   2. comportamiento real de useVista.js, importándolo con un shim mínimo
//      de navegador. Esto también detecta ciclos de import entre composables
//      ("Cannot access ... before initialization"), que node --check no ve;
//   3. contrato entre plantillas, entrypoints y CSS: v-cloak (si falta, el
//      HTML en crudo se pinta antes de montar y se ve el menú de sesión
//      abierto) y puerta de arranque (si se desincroniza, la página se queda
//      en blanco para siempre).
// Falla si una vista se duplica, si un landing no existe, si un nombre no es
// válido para la URL, si la vista visible no se detecta o si un nombre
// desconocido no vuelve a la vista de llegada.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

import { VISTAS, LANDING, PANEL_POR_ROL } from "./vistas.js";

// ---------------------------------------------------------------- 1. datos
const duplicados = VISTAS.filter((v, i) => VISTAS.indexOf(v) !== i);
assert.deepEqual(duplicados, [], `vistas repetidas: ${duplicados.join(", ")}`);

for (const [nombre, slug] of Object.entries(LANDING)) {
    assert.ok(
        VISTAS.includes(slug),
        `LANDING.${nombre}="${slug}" no está en VISTAS: recargar no podría restaurarla`
    );
}

for (const [rol, pagina] of Object.entries(PANEL_POR_ROL)) {
    assert.ok(
        Object.hasOwn(LANDING, pagina.replace(".html", "")),
        `PANEL_POR_ROL.${rol} apunta a "${pagina}", que no tiene LANDING propio`
    );
}

const invalidos = VISTAS.filter(v => !/^[a-z0-9-]+$/.test(v));
assert.deepEqual(invalidos, [], `nombres no válidos en la URL: ${invalidos.join(", ")}`);
assert.ok(VISTAS.length > 0, "VISTAS está vacío");

// ------------------------------------------------------------ 2. shim navegador
const PAGINA = "/frontend/views/administrador.html";
const URL_BASE = `http://127.0.0.1:5500${PAGINA}`;
// Con memoria de verdad: la búsqueda se guarda en sessionStorage y hay que poder
// leerla después para comprobar la persistencia.
const memoria = new Map();
globalThis.sessionStorage = {
    getItem: k => (memoria.has(k) ? memoria.get(k) : null),
    setItem: (k, v) => memoria.set(k, String(v)),
    removeItem: k => memoria.delete(k)
};
// 020: el aviso de logout cross-tab usa localStorage + evento "storage"; se
// simulan ambos (y el reload) para poder comprobar la propagación sin navegador.
const almacen = new Map();
globalThis.localStorage = {
    getItem: k => (almacen.has(k) ? almacen.get(k) : null),
    setItem: (k, v) => almacen.set(k, String(v)),
    removeItem: k => almacen.delete(k)
};
const recargas = [];
const storageListeners = [];
const popstateListeners = [];
const destinos = [];
globalThis.location = {
    hostname: "127.0.0.1",
    href: URL_BASE,
    pathname: PAGINA,
    assign(u) { destinos.push(String(u)); },
    reload() { recargas.push(true); }
};
// Pila de historial mínima: pushState añade una entrada, replaceState sustituye
// la actual. Permite comprobar que Atrás (popstate) no crea pasos de más.
const pila = [URL_BASE];
globalThis.window = {
    location: globalThis.location,
    history: {
        replaceState(_a, _b, u) { globalThis.location.href = String(u); pila[pila.length - 1] = String(u); },
        pushState(_a, _b, u) { globalThis.location.href = String(u); pila.push(String(u)); }
    },
    addEventListener(tipo, fn) {
        if (tipo === "storage") storageListeners.push(fn);
        if (tipo === "popstate") popstateListeners.push(fn);
    }
};
globalThis.history = globalThis.window.history;
globalThis.alert = () => {};
globalThis.confirm = () => true;

const { useVista } = await import("../composables/useVista.js");
const { useModeracion } = await import("../composables/useModeracion.js");
const { useUsuarios } = await import("../composables/useUsuarios.js");
const { useEtiquetas } = await import("../composables/useEtiquetas.js");
const { useGestionEventos } = await import("../composables/useGestionEventos.js");
const { useMisEventos } = await import("../composables/useMisEventos.js");
const { useFavoritos } = await import("../composables/useFavoritos.js");
const { usePerfil } = await import("../composables/usePerfil.js");
const { usePanel } = await import("../composables/usePanel.js");
const { useEventosInvitado } = await import("../composables/useEventosInvitado.js");
const { useEventoForm } = await import("../composables/useEventoForm.js");

const vista = useVista();
const flags = {
    pendientes: () => useModeracion().showPendientes,
    usuarios: () => useUsuarios().showUsuarios,
    etiquetas: () => useEtiquetas().showEtiquetas,
    gestionar: () => useGestionEventos().showManage,
    favoritos: () => useFavoritos().showFavoritos,
    perfil: () => usePerfil().showPerfil,
    panel: () => usePanel().showPanel,
    formulario: () => useEventoForm().showForm,
    "mis-eventos": () => useMisEventos().showMisEventos,
    proximos: () => useEventosInvitado().showProximos,
    busqueda: () => useEventosInvitado().showEvent,
    "sin-resultados": () => useEventosInvitado().showNoEvent,
    portada: () => useEventosInvitado().showCover
};

// Toda vista de VISTAS tiene su flag (si no, visible() no existiría)
assert.deepEqual(
    VISTAS.filter(v => !flags[v]),
    [],
    "hay vistas en VISTAS sin flag asociado en el check"
);

// Página actual deducida de la ruta
assert.equal(vista.paginaActual(), "administrador", "no deduce la página del pathname");

// Sin nada abierto no hay vista que guardar
flags.portada().value = false;
assert.equal(vista.vistaActual.value, "", "con todas las secciones cerradas no debe haber vista");

// Cada sección se detecta con su nombre
for (const [nombre, flag] of Object.entries(flags)) {
    flag().value = true;
    assert.equal(vista.vistaActual.value, nombre, `no se detecta la vista "${nombre}"`);
    vista.verPortada();
    assert.equal(vista.vistaActual.value, "portada", `verPortada() no limpió "${nombre}"`);
}

// La portada manda cuando nada más está abierto
assert.equal(vista.vistaActual.value, "portada", "la portada debe ser la vista por defecto");

// verPortada() apaga todas las banderas (ninguna sobrevive)
const encendidas = Object.entries(flags)
    .filter(([nombre]) => nombre !== "portada")
    .filter(([, f]) => f().value)
    .map(([nombre]) => nombre);
assert.deepEqual(encendidas, [], `verPortada() dejó visibles: ${encendidas.join(", ")}`);

// Un nombre desconocido (URL manipulada) vuelve a la vista de llegada, no a
// un panel ajeno ni a una página vacía
vista.verPortada();
vista.abrir("no-existe");
assert.equal(vista.vistaActual.value, "portada", "un nombre desconocido debe caer en el landing");

// La URL se refleja y no rompe la ruta (pushState por navegación real)
useModeracion().showPendientes.value = true;
await new Promise(r => setTimeout(r, 0)); // el watch de Vue es asíncrono
assert.match(globalThis.location.href, /\?vista=pendientes$/, "la URL no refleja la vista visible");

// La cabecera (@click.prevent="irAInicio()") resuelve contra lo que useAuth
// expone a las plantillas: si esto falla, el clic no hace nada.
const { useAuth } = await import("../composables/useAuth.js");
assert.equal(typeof useAuth().irAInicio, "function", "useAuth no expone irAInicio()");
assert.equal(typeof useAuth().irAPanel, "function", "useAuth no expone irAPanel()");
assert.equal(typeof useAuth().irAPerfil, "function", "useAuth no expone irAPerfil()");

// ------------------------------------------------- 2b. buscar(): dos formas
// GET /eventos responde Page<> sin ?fecha y una lista plana con ?fecha. Si
// buscar() asume Page<>, con fecha r.data.content es undefined, el .map revienta
// con TypeError y searchEvent() lo se traga en su catch: la búsqueda por fecha
// (invitado y admin) no muestra nada y no avisa. Se ejecuta la función real.
let respuesta = { content: [{ id: 1, fecha: "2026-10-02" }], totalPages: 1, totalElements: 1, first: true, last: true, number: 0 };
globalThis.fetch = async () => ({ ok: true, status: 200, json: async () => respuesta });

const { buscar } = await import("../api/eventos.js");
assert.equal((await buscar({ page: 0 })).content.length, 1, "buscar() debe pasar el contenido cuando llega un Page<>");

respuesta = [{ id: 2, fecha: "2026-10-02" }]; // forma real con ?fecha
let conLista = null, fallo = null;
try { conLista = await buscar({ fecha: "2026-10-02" }); } catch (e) { fallo = e; }
assert.equal(fallo, null, `buscar() con ?fecha debe normalizar la lista plana del backend; ha reventado con: ${fallo && fallo.message}`);
assert.equal(conLista.content.length, 1, "buscar() con ?fecha debe normalizar la lista plana: si no, la búsqueda por fecha no muestra nada");
assert.equal(conLista.totalElements, 1, "buscar() debe rellenar la metainformación al normalizar la lista");

respuesta = { error: "Los parámetros fecha y futuros son excluyentes" }; // 400 sin ok
const conError = await buscar({ fecha: "2026-10-02", futuros: true });
assert.equal(conError.content.length, 0, "buscar() con un 400 debe devolver la página vacía, no reventar");

// ------------------------------------------------- 3. puerta de arranque
// <main> está oculto mientras arrancando es true: es lo que evita el parpadeo
// de la portada al recargar. Si esto no cuadra entre plantilla y entrypoint,
// la página se queda en blanco para siempre.
assert.equal(vista.arrancando.value, true, "la puerta de arranque debe empezar cerrada");
vista.listo();
assert.equal(vista.arrancando.value, false, "listo() debe abrir la puerta");

const plantilla = n => readFileSync(new URL(`../../views/${n}.html`, import.meta.url), "utf8");
const entrypoint = n => readFileSync(new URL(`../pages/${n}.js`, import.meta.url), "utf8");
const login = readFileSync(new URL("../../index.html", import.meta.url), "utf8");
const css = readFileSync(new URL("../../css/eventos.css", import.meta.url), "utf8");
const reglaCargando = css.match(/\.cargando\s*\{[^}]*\}/);
assert.ok(reglaCargando, "eventos.css no define .cargando: el aviso saldría sin estilo");
// El aviso tiene que ocupar la ventana. <main> está oculto mientras carga, así
// que sin esto el documento se encoge, el pie sale pegado a la cabecera y
// luego se desliza al aparecer el contenido (CLS). Un calc con la altura de
// cabecera y pie estimado a ojo también valdría, pero se pudre: aquí se exige
// viewport para que la decisión sea explícita.
assert.match(
    reglaCargando[0], /min-height:\s*100d?vh/,
    ".cargando no ocupa la ventana: el pie se encoge arriba y luego se desliza (CLS)"
);
assert.match(
    reglaCargando[0], /place-items:\s*center|justify-content:\s*center/,
    ".cargando no centra el aviso: con la ventana llena quedaría pegado al borde"
);
// El spinner es un ::before animado: sin el bloque de reduced-motion es un
// Accessibility Warning en el Lighthouse y molesta a quien lo tenga activado.
assert.ok(
    /@media\s*\(prefers-reduced-motion:\s*reduce\)/.test(css),
    "eventos.css no respeta prefers-reduced-motion: el spinner de carga giraría siempre"
);
assert.ok(
    /scrollbar-gutter:\s*stable/.test(css),
    "sin scrollbar-gutter el aviso de carga hace aparecer y desaparecer la barra (~15px de shift)"
);
assert.ok(
    css.includes("[v-cloak] {") && /\[v-cloak\]\s*\{\s*display:\s*none/.test(css),
    "eventos.css no oculta [v-cloak]: volvería el fogonazo del HTML crudo"
);

// v-cloak en el contenedor que se monta: sin esto, el script diferido deja
// pintar el HTML literal (todas las secciones y el menú de sesión abiertos)
// antes de que Vue aplique un solo v-if.
for (const [nombre, html] of [
    ["index.html", login],
    ["administrador.html", plantilla("administrador")],
    ["organizador.html", plantilla("organizador")],
    ["usuarioEstandar.html", plantilla("usuarioEstandar")]
]) {
    assert.ok(
        html.includes('<div id="eventos" v-cloak>'),
        `${nombre}: #eventos sin v-cloak, se pintará el HTML crudo antes del montaje`
    );
    assert.ok(html.includes("css/eventos.css"), `${nombre} no carga eventos.css`);
}

for (const [pagina, vista] of [
    ["administrador", "admin"],
    ["organizador", "organizador"],
    ["usuarioEstandar", "invitado"]
]) {
    const html = plantilla(pagina);
    assert.ok(html.includes('v-show="!arrancando"'), `${pagina}.html: <main> sin v-show="!arrancando"`);
    assert.ok(html.includes('v-show="arrancando"'), `${pagina}.html: sin aviso de carga`);
    // La cabecera entra en la misma puerta: si no, el botón saldría como
    // "Mi cuenta" y luego cambiaría al nombre, y su ancho recolocaría el header.
    assert.ok(
        /<template v-if="!arrancando">\s*<div class="sesion"/.test(html),
        `${pagina}.html: el bloque de sesión de la cabecera no está tras la puerta de arranque`
    );
    const js = entrypoint(vista);
    assert.ok(
        js.includes("arrancando: vista.arrancando"),
        `pages/${vista}.js no expone arrancando a la plantilla: <main> nunca se vería`
    );
    assert.match(
        js, /finally\s*\{\s*vista\.listo\(\);/,
        `pages/${vista}.js: listo() fuera del finally, una petición fallida dejaría la página en blanco`
    );
}

// restaurar() avisa de si tuvo que abrir algo, para que la página se salte su
// carga base cuando la vista restaurada ya se ha encargado de ella.
globalThis.location.href = URL_BASE;
assert.equal(await vista.restaurar(), false, "sin ?vista= no hay nada que restaurar");
// "formulario" y no "usuarios": su opener es estado puro, sin peticiones al backend.
globalThis.location.href = `${URL_BASE}?vista=formulario`;
assert.equal(await vista.restaurar(), true, "con ?vista= debe restaurar la sección");
assert.equal(vista.vistaActual.value, "formulario", "restaurar() no abrió la sección pedida");

// Una búsqueda no se puede reaplicar sin filtros guardados: cae en la portada y
// la URL se corrige, en vez de dejar la URL diciendo una vista que no es la real.
memoria.delete("vista:busqueda");
globalThis.location.href = `${URL_BASE}?vista=busqueda`;
assert.equal(await vista.restaurar(), true);
assert.equal(vista.vistaActual.value, "portada", "una búsqueda sin filtros debe caer en la portada");
assert.match(
    globalThis.location.href, /\?vista=portada$/,
    "la URL debe describir la vista real tras restaurar"
);

// Los filtros de búsqueda sobreviven al recargar (sessionStorage, no la URL).
const inv = useEventosInvitado();
inv.searchDate.value = "2026-12-01";
inv.searchEtiquetas.value = [];
await new Promise(r => setTimeout(r, 0));
assert.deepEqual(
    JSON.parse(memoria.get("vista:busqueda")),
    { fecha: "2026-12-01", etiquetas: [] },
    "los filtros de búsqueda no se guardan, se perderían al recargar"
);

// La cabecera va a index.html cuando no hay sesión: el invitado no tiene panel
// al que volver, y LANDING[usuarioEstandar] lo devolvía a la cartelera en el
// sitio, así que el clic no llevaba a ninguna parte. Con sesión, en cambio, debe
// resetear SIN navegar: pasar por index.html pintaba el login antes de rebotar.
const { store, avisarCierreSesion } = await import("../store.js");
destinos.length = 0;
store.token = "";
vista.irAInicio();
assert.deepEqual(
    destinos, ["../index.html"],
    "sin sesión, la cabecera debe llevar a index.html (el invitado no tiene panel al que volver)"
);

destinos.length = 0;
store.token = "jwt.de.prueba";
vista.verPortada();
vista.irAInicio();
assert.deepEqual(destinos, [], "con sesión, la cabecera no debe navegar: resetea en el sitio");
assert.equal(
    vista.vistaActual.value, LANDING.administrador,
    "con sesión, la cabecera debe abrir el landing de su propia página"
);
store.token = "";

// 020: cerrar sesión en una pestaña avisa a las demás (evento "storage" de
// localStorage): cada otra pestaña limpia su token de sessionStorage y recarga,
// y su guardia la manda al login. Sin esto, la otra pestaña seguiría "logueada"
// con su propio token de sessionStorage hasta que caducara.
avisarCierreSesion();
assert.ok(
    globalThis.localStorage.getItem("sesion:logout"),
    "avisarCierreSesion() no emite la señal de logout cross-tab"
);
memoria.set("token", "jwt.de.prueba");
store.token = "jwt.de.prueba";
recargas.length = 0;
storageListeners.forEach(fn => fn({ key: "sesion:logout" }));
assert.equal(
    globalThis.sessionStorage.getItem("token"), null,
    "el aviso de logout cross-tab no limpió el token de esta pestaña"
);
assert.equal(store.token, "", "el aviso de logout cross-tab no limpió el store");
assert.deepEqual(
    recargas, [true],
    "el aviso de logout cross-tab no recargó la pestaña para que la guardia la mande al login"
);

// Atrás entre secciones: popstate con la URL de la entrada anterior reabre ESA
// sección (no la URL pelada) y no añade un paso nuevo al historial.
assert.equal(popstateListeners.length, 1, "useVista no registró el listener de popstate");
globalThis.location.href = URL_BASE; // entrada de llegada
vista.verPortada();
await new Promise(r => setTimeout(r, 0));
vista.abrir("formulario");
await new Promise(r => setTimeout(r, 0));
assert.equal(vista.vistaActual.value, "formulario", "no se abrió el formulario para la prueba de Atrás");
const pasos = pila.length;
globalThis.location.href = `${URL_BASE}?vista=portada`; // simula la entrada anterior
popstateListeners.forEach(fn => fn());
await new Promise(r => setTimeout(r, 0));
assert.equal(vista.vistaActual.value, "portada", "Atrás debe volver a la sección de la entrada anterior");
assert.equal(pila.length, pasos, "popstate no debe crear una entrada de historial nueva");

console.log(
    `OK  ${VISTAS.length} vistas, ${Object.keys(LANDING).length} landings, ` +
    `${Object.keys(PANEL_POR_ROL).length} roles, ${Object.keys(flags).length} flags comprobadas, ` +
    `puerta de arranque en 3 páginas`
);
