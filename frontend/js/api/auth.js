// SPDX-License-Identifier: MIT

// Repository de autenticación y usuarios (Fase 2 de 008-refactor-modular-esm).
// Mueve los fetch de validateAdmin, register, logout, cargarUsuarios (+perfil),
// crearAdmin, editarRolesUsuario y toggleActivo desde eventos.js, misma URL/método/body,
// usando authFetch en vez del try/catch repetido. Sin estado UI: devuelve datos o
// { ok:false, ... }; el texto de cada error lo pone el composable (Fase 4).
// Todavía sin uso en ninguna página — solo existe, verificable con node --check.
import { API, authHeader, authFetch } from './http.js';

// POST /auth/login — 401 aquí = credenciales malas (no sesión caducada).
// Devuelve el body tal cual ({ accessToken|token, user:{ roles } }) o null si !ok.
export async function login(email, password) {
    const r = await authFetch(`${API}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include", // la cookie de refresh es cross-site en producción
        body: JSON.stringify({ email, password }),
        authFail: "return"
    });
    return r.ok ? r.data : null;
}

// POST /auth/register — endpoint público; el error viaja en campo, sin alerta.
export async function register({ email, password, nombre, apellidos, telefono }) {
    const r = await authFetch(`${API}/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password, nombre, apellidos, telefono }),
        networkAlert: false,
        authFail: "return"
    });
    if (r.ok) return { ok: true };
    // La validación de Spring devuelve {campo: mensaje}, no {error}: si no se
    // traduce aquí el usuario ve "No se pudo crear la cuenta" sin saber por qué.
    return { ok: false, error: mensajeValidacion(r.data) || "No se pudo crear la cuenta" };
}

function mensajeValidacion(data) {
    if (!data) return null;
    if (data.error) return data.error;
    if (data.password) return data.password;
    if (data.email) return data.email;
    const resto = Object.values(data);
    return resto.length ? resto[0] : null;
}

// POST /auth/logout — traga cualquier fallo (sin red, igual se limpia la sesión local).
export async function logout() {
    try {
        await authFetch(`${API}/auth/logout`, {
            method: "POST",
            credentials: "include", // para que viaje la cookie de refresh a revocar
            parseJson: false,
            networkAlert: false,
            authFail: "return"
        });
    } catch (e) {
        // Si no hay red, igual se limpia la sesión local
    }
}

// GET /auth/usuarios + GET /auth/perfil (email propio, silencioso si falla).
export async function listarUsuarios() {
    const r = await authFetch(`${API}/auth/usuarios`, { headers: { ...authHeader() } });
    let miEmail = "";
    try {
        const p = await authFetch(`${API}/auth/perfil`, {
            headers: { ...authHeader() },
            networkAlert: false,
            authFail: "return"
        });
        if (p.ok) miEmail = (p.data && p.data.email) || "";
    } catch (e) {
        miEmail = "";
    }
    return { usuarios: r.data, miEmail };
}

// POST /auth/usuarios-admin — el error viaja en campo, sin alerta.
export async function crearAdmin({ email, password, nombre, apellidos, telefono }) {
    const r = await authFetch(`${API}/auth/usuarios-admin`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...authHeader() },
        body: JSON.stringify({ email, password, nombre, apellidos, telefono }),
        networkAlert: false
    });
    if (r.ok) return { ok: true };
    return { ok: false, error: mensajeValidacion(r.data) || "No se pudo crear" };
}

// PUT /auth/perfil — edita los datos propios (nunca rol/estado); el error viaja en campo.
// 023: mensajeValidacion y no solo `.error`, porque el email ahora pasa por @Valid y
// un formato inválido vuelve como {email: "..."}. Con solo `.error` el usuario veía
// "No se pudo guardar" sin saber qué corregir.
// 023: authFail "return" porque el 403 de la cuenta demo trae mensaje propio que hay que
// enseñar. Con el "redirect" por defecto, http.js lo trataba como sesión caducada (alerta,
// borra el token y manda al login) y el perfil cerraba la sesión en vez de explicar nada:
// eso incumplía SC-003 de la spec 023. El 401 sí lo gestiona el composable.
export async function actualizarPerfil(body) {
    const r = await authFetch(`${API}/auth/perfil`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", ...authHeader() },
        body: JSON.stringify(body),
        networkAlert: false,
        authFail: "return"
    });
    if (r.ok) return { ok: true, perfil: r.data };
    return { ok: false, status: r.status, error: mensajeValidacion(r.data) || "No se pudo guardar" };
}

// PUT /auth/usuarios/{id} — body { roles } o { enabled }; el error viaja en campo.
// Devuelve también status (el 409 de auto-bloqueo lo distingue el composable).
// error puede venir undefined: cada llamante aplica su fallback literal de hoy.
export async function actualizarUsuario(id, body) {
    const r = await authFetch(`${API}/auth/usuarios/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", ...authHeader() },
        body: JSON.stringify(body),
        networkAlert: false
    });
    if (r.ok) return { ok: true };
    return { ok: false, status: r.status, error: r.data && r.data.error };
}
