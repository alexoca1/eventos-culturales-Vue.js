// Self-check del perfil (023). Se ejecuta con:
//     node js/ui/perfil.check.js
// Sin framework ni fixtures. Lo que hay que garantizar aquí es que cambiar el email
// no deje al usuario fuera: el `sub` del JWT es el email, así que al guardarlo el
// token viejo deja de identificar a nadie y hay que renovarlo con la cookie.
// Esto también detecta el ciclo de import entre usePerfil y useAuth
// ("Cannot access ... before initialization"), que node --check no ve.
import assert from "node:assert/strict";

// ------------------------------------------------------------- shim navegador
const PAGINA = "/frontend/views/usuarioEstandar.html";
const memoria = new Map();
globalThis.sessionStorage = {
    getItem: k => (memoria.has(k) ? memoria.get(k) : null),
    setItem: (k, v) => memoria.set(k, String(v)),
    removeItem: k => memoria.delete(k)
};
globalThis.localStorage = {
    getItem: () => null,
    setItem: () => {},
    removeItem: () => {}
};
globalThis.location = {
    hostname: "127.0.0.1",
    href: `http://127.0.0.1:5500${PAGINA}`,
    pathname: PAGINA,
    assign() {},
    reload() {}
};
globalThis.window = {
    location: globalThis.location,
    history: { replaceState() {}, pushState() {} },
    addEventListener() {}
};
const alertas = [];
globalThis.alert = (...args) => { alertas.push(args.join(" ")); };
globalThis.confirm = () => true;

const TOKEN_NUEVO = "jwt-con-el-email-nuevo";
const llamadas = [];
const cuerpos = [];
// Respuesta forzada para el PUT: permite reproducir el 403 de la cuenta demo y el
// 401 de un token caducado, que son los dos caminos de error que hay que cubrir.
let respuestaPut = null;
// Borrado de cuenta (RGPD): por defecto DELETE /auth/perfil responde 200; con este
// dial se reproduce el error del backend y el 401 del token caducado.
let respuestaDelete = null;
let emailEnBackend = "ana@test.com";

const json = (status, body) => ({
    ok: status >= 200 && status < 300,
    status,
    json: async () => body
});

globalThis.fetch = async (url, opts = {}) => {
    const u = String(url);
    const metodo = (opts.method || "GET").toUpperCase();
    // la cabecera viaja en el registro: el DELETE debe llevar el JWT de la sesión
    const bearer = opts.headers && opts.headers.Authorization ? " " + opts.headers.Authorization : "";
    llamadas.push(`${metodo} ${u}${bearer}`);
    if (opts.body) cuerpos.push(opts.body);
    if (u.endsWith("/auth/refresh")) {
        return json(200, { accessToken: TOKEN_NUEVO, user: { roles: ["ROLE_USER"], email: emailEnBackend } });
    }
    if (respuestaPut && metodo === "PUT") {
        return json(respuestaPut.status, respuestaPut.body);
    }
    if (respuestaDelete && metodo === "DELETE") {
        return json(respuestaDelete.status, respuestaDelete.body);
    }
    if (u.endsWith("/auth/perfil")) {
        return json(200, { email: emailEnBackend, nombre: "Ana", apellidos: "Ruiz", telefono: "600000000", roles: ["ROLE_USER"] });
    }
    return json(200, {});
};

// Los dos composables se importan JUNTOS a propósito: usePerfil usa a useAuth para
// renovar la sesión y useAuth ya usaba a usePerfil. Si ese ciclo no fuese seguro,
// el import (o la primera llamada) exploding aquí.
const { usePerfil } = await import("../composables/usePerfil.js");
const { useAuth } = await import("../composables/useAuth.js");
const { store, setSession } = await import("../store.js");

const perfil = usePerfil();
const auth = useAuth();
assert.equal(perfil, usePerfil(), "usePerfil debe ser un singleton");
assert.equal(auth, useAuth(), "useAuth debe ser un singleton");
assert.equal(typeof auth.renovarSesion, "function", "useAuth debe exponer renovarSesion (023)");

const PERFIL = { email: "ana@test.com", nombre: "Ana", apellidos: "Ruiz", telefono: "600000000" };

// Rellenar deja el email a la vista y rememberiza el que hay en BD: sin ese segundo
// valor no se puede distinguir "guardé lo mismo" de "lo cambié".
await perfil.abrirPerfil(PERFIL);
assert.equal(perfil.showPerfil.value, true, "abrirPerfil debe mostrar el formulario");
assert.equal(perfil.perfilEmail.value, "ana@test.com", "perfilEmail se rellena desde GET /auth/perfil");

// --- guardar sin tocar el email: ni un refresh de más -------------------------
llamadas.length = 0;
await perfil.guardarPerfil(false);
assert.equal(perfil.perfilOk.value, true, "guardar sin cambios debe salir ok");
assert.equal(perfil.perfilError.value, "", "sin errores al guardar");
assert.ok(llamadas.some(c => c.startsWith("PUT")), "debe llamar a PUT /auth/perfil");
assert.ok(!llamadas.some(c => c.includes("/auth/refresh")),
    "guardar sin cambiar el email no debe gastar la cookie de refresh");

// --- cambiar el email: PUT y luego renovación, con el token nuevo --------------
llamadas.length = 0;
emailEnBackend = "nuevo@test.com";
perfil.perfilEmail.value = "nuevo@test.com";
await perfil.guardarPerfil(false);

assert.equal(perfil.perfilOk.value, true, "cambiar el email debe salir ok");
assert.ok(llamadas.some(c => c.startsWith("PUT")), "primero el PUT del perfil");
assert.ok(llamadas.some(c => c.startsWith("POST") && c.includes("/auth/refresh")),
    "tras cambiar el email hay que renovar la sesión (el `sub` del JWT es el email)");
assert.equal(store.token, TOKEN_NUEVO, "el token guardado debe ser el nuevo");
assert.equal(store.miEmail, "nuevo@test.com", "el email de sesión debe ser el nuevo");
assert.equal(auth.sesionNombre.value, "nuevo@test.com", "la cabecera debe enseñar el email nuevo");

// Un segundo guardado ya no renueva: emailGuardado quedó actualizado.
llamadas.length = 0;
await perfil.guardarPerfil(false);
assert.ok(!llamadas.some(c => c.includes("/auth/refresh")),
    "guardar dos veces sin volver a cambiar el email no debe renovar otra vez");

// --- el email viaja en el cuerpo del PUT -------------------------------------
// Si `email` se cayera del body, el backend lo interpretaría como "no lo toco" y
// la feature no haría nada en silencio.
cuerpos.length = 0;
perfil.perfilEmail.value = "nuevo2@test.com";
await perfil.guardarPerfil(false);
const cuerpo = JSON.parse(cuerpos[0]);
assert.equal(cuerpo.email, "nuevo2@test.com", "el email debe enviarse en el PUT");
assert.equal(cuerpo.telefono, "600000000", "el resto de campos sigue igual");

// --- 403 de la cuenta demo: el mensaje va al campo y la sesión se queda ----------
// Con el authFail "redirect" que traía por defecto, http.js convertía este 403 en
// alerta "Sesión caducada" + token borrado + salto al login: el perfil echaba al
// usuario en vez de explicar que esa cuenta no puede cambiar su email (SC-003).
llamadas.length = 0;
alertas.length = 0;
perfil.perfilError.value = "";
perfil.perfilOk.value = false;
const tokenAntes = sessionStorage.getItem("token");
respuestaPut = {
    status: 403,
    body: { error: "Acción no permitida en modo demostración. Esta cuenta tiene permisos limitados para proteger los datos." }
};
await perfil.guardarPerfil(false);
respuestaPut = null;
assert.equal(perfil.perfilOk.value, false, "el 403 no puede dar por guardado");
assert.match(perfil.perfilError.value, /modo demostraci/,
    "el mensaje del backend debe llegar al campo en rojo");
assert.equal(alertas.length, 0, "el 403 no debe disparar la alerta de sesión caducada (SC-003)");
assert.equal(sessionStorage.getItem("token"), tokenAntes, "el 403 no debe borrar el token");
assert.ok(!llamadas.some(c => c.includes("/auth/refresh")), "un 403 no renueva la sesión");

// --- 401: token caducado de verdad → cerrar sesión (actualizarPerfil ya no redirige)
llamadas.length = 0;
respuestaPut = { status: 401, body: {} };
await perfil.guardarPerfil(false);
respuestaPut = null;
assert.equal(sessionStorage.getItem("token"), null,
    "con 401 hay que cerrar sesión, igual que hacía http.js por defecto");
assert.ok(llamadas.some(c => c.startsWith("POST") && c.includes("/auth/logout")),
    "el 401 debe pasar por el logout normal");

console.log("OK  023: el email se edita, la sesión se renueva sola y el 403 de la demo no echa al usuario");

// --- borrado de la cuenta (RGPD): DELETE /auth/perfil -------------------------
// Flujo completo: diálogo → DELETE con el JWT → sesión fuera → inicio con el aviso.
// Lo más fácil de romper es el error del backend: debe dejar la cuenta y la sesión
// donde estaban (mostrando el motivo), no echar al usuario como hacía por defecto
// el authFail "redirect".
auth.pedirBorrado();
assert.equal(auth.confirmarBorrado.value, true, "pedirBorrado debe abrir la confirmación");
auth.cancelarBorrado();
assert.equal(auth.confirmarBorrado.value, false, "cancelarBorrado debe cerrarla sin borrar nada");
assert.equal(auth.avisoCuentaEliminada.value, false,
    "sin ?cuentaEliminada=1 la página de acceso no debe enseñar el aviso");

// El 401 anterior dejó la cuenta sin sesión: hace falta una para poder borrarla
setSession("jwt-que-no-se-borra", { roles: ["ROLE_USER"], miEmail: "ana@test.com" });
location.href = "http://127.0.0.1:5500/frontend/views/usuarioEstandar.html";

// 1) error del backend: en la página, con el motivo a la vista y la sesión intacta
llamadas.length = 0;
respuestaDelete = { status: 400, body: { error: "No se pudo eliminar la cuenta" } };
auth.pedirBorrado(); // el flujo real: el usuario abre la confirmación y pulsa Sí
await auth.eliminarCuenta();
assert.match(auth.borradoError.value, /No se pudo eliminar/, "el motivo del backend debe verse en el diálogo");
assert.equal(auth.confirmarBorrado.value, true, "el diálogo se queda abierto para leer el error");
assert.equal(sessionStorage.getItem("token"), "jwt-que-no-se-borra",
    "un error no debe cerrar la sesión ni borrar la cuenta");
assert.equal(location.href, "http://127.0.0.1:5500/frontend/views/usuarioEstandar.html",
    "un error no debe redirigir");

// 2) 401: token caducado → misma salida que en el resto del perfil
llamadas.length = 0;
respuestaDelete = { status: 401, body: {} };
auth.pedirBorrado(); // el flujo real: el usuario abre la confirmación y pulsa Sí
await auth.eliminarCuenta();
assert.equal(sessionStorage.getItem("token"), null, "con 401 hay que cerrar la sesión");
assert.ok(llamadas.some(c => c.startsWith("POST") && c.includes("/auth/logout")),
    "el 401 debe pasar por el logout normal");

// 3) OK: sesión fuera e inicio con el parámetro que dispara el aviso
setSession("jwt-que-si-se-borra", { roles: ["ROLE_USER"], miEmail: "ana@test.com" });
location.href = "http://127.0.0.1:5500/frontend/views/usuarioEstandar.html";
llamadas.length = 0;
respuestaDelete = null;
auth.pedirBorrado(); // el flujo real: el usuario abre la confirmación y pulsa Sí
await auth.eliminarCuenta();
assert.ok(llamadas.some(c => c.startsWith("DELETE") && c.includes("/auth/perfil")
    && c.includes("Bearer jwt-que-si-se-borra")), "el DELETE debe llevar el JWT de la sesión");
assert.equal(sessionStorage.getItem("token"), null, "borrada la cuenta se limpia el token");
assert.equal(store.token, "", "y el token del store");
assert.equal(location.href, "../index.html?cuentaEliminada=1",
    "hay que ir al inicio con ?cuentaEliminada=1, que es lo que dispara el aviso");
respuestaDelete = null;

console.log("OK  RGPD: borrado con confirmación, DELETE con JWT, error sin echar y aviso en el destino");