// Capa base de red (Fase 1 de 008-refactor-modular-esm).
// Centraliza el patrón que hoy está repetido ~15 veces en eventos.js:
// error de red (alerta) + 401/403 (alerta, limpia token, redirige) + parseo de error.
// Todavía sin uso en ninguna página (ver tasks.md) — solo existe, verificable con node --check.

// En producción apunta al backend desplegado (Render). En local/file:// usa localhost.
// (Movido tal cual desde eventos.js: misma lógica de detección, mismo placeholder.)
const RENDER_API = "https://TU-BACKEND.onrender.com";
// En local se usa el MISMO host que sirve la página (localhost o 127.0.0.1):
// así la cookie de refresh queda same-site y el navegador la guarda/envía,
// tanto si se abre en http://localhost:5500 como en http://127.0.0.1:5500.
export const API = (location.hostname === "" || location.hostname === "localhost" || location.hostname === "127.0.0.1")
    ? `http://${location.hostname || "localhost"}:8081`
    : RENDER_API;

// (Movida tal cual desde eventos.js.)
export function authHeader() {
    return { "Authorization": "Bearer " + sessionStorage.getItem("token") };
}

// El backend no responde (red caída, backend apagado). Equivale al catch actual:
// alert("No se pudo conectar con el servidor. ¿Está arrancado el backend?")
export class NetworkError extends Error {}

// 401/403 tras limpiar token y redirigir al login. Se lanza para que el llamante
// no siga procesando la respuesta (equivale al `return`/`return false` actual).
export class AuthExpiredError extends Error {}

// Llamada autenticada a la API.
// opts: { method, headers, body, credentials, loginPage, parseJson, authFail, networkAlert }
// - loginPage: destino tras 401/403. "../index.html" por defecto (páginas de views/);
//   "index.html" para los flujos de usuarioEstandar.html (mismo valor literal que hoy).
// - parseJson (default true): intenta res.json(); si no hay JSON, data = null.
// - authFail (default "redirect"): "redirect" = alerta + limpia token + redirige + lanza
//   AuthExpiredError; "return" = devuelve { ok:false, status, data } sin tocar la sesión
//   (para login/register: un 401 ahí son credenciales malas, no sesión caducada).
// - networkAlert (default true): en false no alerta en fallo de red, solo lanza
//   NetworkError (para los sitios que ponen el error en un campo: register, crearAdmin...).
// Devuelve { ok, status, data }. En !ok no crítico, data = JSON o {} (mismo
// `.catch(() => ({}))` actual) para que cada llamante componga su mensaje.
export async function authFetch(url, opts = {}) {
    const { loginPage = "../index.html", parseJson = true, authFail = "redirect",
        networkAlert = true, ...fetchOpts } = opts;
    let res;
    try {
        res = await fetch(url, fetchOpts);
    } catch (e) {
        if (networkAlert) alert("No se pudo conectar con el servidor. ¿Está arrancado el backend?");
        throw new NetworkError("backend no alcanzable");
    }
    if ((res.status === 401 || res.status === 403) && authFail === "redirect") {
        alert("Sesión caducada o sin permisos. Vuelve a entrar.");
        sessionStorage.removeItem("token");
        window.location.href = loginPage;
        throw new AuthExpiredError("sesión caducada o sin permisos");
    }
    let data = null;
    if (parseJson) {
        data = await res.json().catch(() => (res.ok ? null : {}));
    }
    return { ok: res.ok, status: res.status, data };
}
