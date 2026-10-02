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
    return { ok: false, error: (r.data && r.data.error) || "No se pudo crear la cuenta" };
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
    return { ok: false, error: (r.data && r.data.error) || "No se pudo crear" };
}

// PUT /auth/perfil — edita los datos propios (nunca rol/estado); el error viaja en campo.
export async function actualizarPerfil(body) {
    const r = await authFetch(`${API}/auth/perfil`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", ...authHeader() },
        body: JSON.stringify(body),
        networkAlert: false
    });
    if (r.ok) return { ok: true, perfil: r.data };
    return { ok: false, status: r.status, error: (r.data && r.data.error) || "No se pudo guardar" };
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
