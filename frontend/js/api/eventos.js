// Repository de eventos y favoritos (Fase 2 de 008-refactor-modular-esm).
// Mueve apiToView (copiado tal cual, sin cambios de lÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³gica) y los fetch de searchEvent,
// loadAllEvents, submitForm, deleteOne, cargarPendientes, aprobar, confirmarRechazo,
// loadMisEventos, verFavoritos, toggleFavorito y loadFavoritos desde eventos.js,
// misma URL/mÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©todo/body, usando authFetch en vez del try/catch repetido.
// TodavÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â­a sin uso en ninguna pÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡gina ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â solo existe, verificable con node --check.
import { API, authHeader, authFetch } from './http.js';

// API (establecimiento, direccion, fecha...) -> vista (establishment, address, date...)
// El cartel siempre se sirve desde el backend (bytes en BD o redirecciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n a URL externa)
export function apiToView(e) {
    return {
        id: e.id,
        establishment: e.establecimiento,
        address: e.direccion,
        date: e.fecha,
        // 019: `nombre` es el título corto y obligatorio del evento; `descripcion` es
        // el texto largo opcional. `content` sigue siendo el nombre visible en las
        // tarjetas (alias heredado que usan muchas plantillas), ya no la descripción.
        nombre: e.nombre || "",
        content: e.nombre || "",
        description: e.descripcion || "",
        // 019: contacto y redes (F189). `redes` = [{red, url}] tal cual llegan del
        // backend; vacío si el evento no declara ninguna.
        redes: e.redesSociales || [],
        telefono: e.telefonoEvento || null,
        urlEvento: e.urlEvento || null,
        // Rellenado aparte por cargarGalerias(): el listado no trae los índices.
        galeriaIndices: [],
        etiquetas: (e.etiquetas || []).map(t => t.nombre),
        estado: e.estado || null,
        // Punto 4: el nombre visible es el de la organizaciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n si lo tiene, si no el email
        creadoPor: e.creadoPor ? (e.creadoPor.nombreOrganizacion || e.creadoPor.email) : null,
        motivoRechazo: e.motivoRechazo || null,
        horaInicio: e.horaInicio || null,
        horaFin: e.horaFin || null,
        fechaFin: e.fechaFin || e.fecha,
        // Sin cartel en BD (eventos que entraron por API sin poster) el src da 404: el
        // onerror lo cambia por el banner de portada en vez de un icono de imagen rota.
        poster: `<img src="${API}/eventos/${e.id}/cartel" alt="evento imagen" style="max-width: 400px; max-height: 400px;" onerror="this.onerror=null;this.src='../img/evento_portada.jpg'">`,
        map: e.mapaEmbed || ""
    };
}

// GET /eventos?fecha=&etiquetas=&futuros=&page= ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â pÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âºblico, sin cabecera de auth.
// etiquetas: array ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ ?etiquetas=a,b (ANY). page: base 0 (se omite si es null).
// Devuelve el Page<> completo con content mapeado a vista.
export async function buscar({ fecha, etiquetas, futuros, page }) {
    const params = new URLSearchParams();
    if (fecha) params.append("fecha", fecha);
    if (etiquetas && etiquetas.length) params.append("etiquetas", etiquetas.join(","));
    if (futuros) params.append("futuros", "true");
    if (page != null) params.append("page", page);
    const r = await authFetch(`${API}/eventos?${params}`, { authFail: "return" });
    // El backend responde Page<> sin ?fecha y una lista plana con ?fecha
    // (findVigentesEn* no pagina; docs/api-contract.md:11). Sin normalizar, el
    // .map revienta con TypeError y searchEvent() se lo traga en su catch: la
    // bÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âºsqueda por fecha no muestra nada y tampoco avisa de nada.
    if (!r.ok) return paginaVacia();
    const data = Array.isArray(r.data)
        ? { ...paginaVacia(), content: r.data, totalElements: r.data.length }
        : r.data;
    if (!Array.isArray(data.content)) return paginaVacia();
    return { ...data, content: data.content.map(apiToView) };
}

// 020: GET /eventos?q=&page= — búsqueda por texto (nombre o establecimiento),
// pública y sin cabecera de auth (igual que buscar() y proximos()). El backend
// responde un Page<> con ?q, así que se mapea content a vista (la plantilla y
// cargarGalerias() lo necesitan); en fallo, página vacía.
export async function buscarTexto({ q, page = 0 }) {
    const r = await authFetch(`${API}/eventos?q=${encodeURIComponent(q)}&page=${page}`,
        { authFail: "return", networkAlert: false });
    if (!r.ok) return paginaVacia();
    const data = Array.isArray(r.data)
        ? { ...paginaVacia(), content: r.data, totalElements: r.data.length }
        : r.data;
    if (!Array.isArray(data.content)) return paginaVacia();
    return { ...data, content: data.content.map(apiToView) };
}

// GET /etiquetas ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â catÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡logo pÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âºblico y silencioso (si falla, []).
export async function listarEtiquetas() {
    try {
        const r = await authFetch(`${API}/etiquetas`, { networkAlert: false, authFail: "return" });
        if (!r.ok) return [];
        return r.data;
    } catch (e) {
        return [];
    }
}

// POST /etiquetas (admin).
export async function crearEtiqueta(nombre) {
    const r = await authFetch(`${API}/etiquetas`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...authHeader() },
        body: JSON.stringify({ nombre }),
        networkAlert: false
    });
    if (r.ok) return { ok: true, etiqueta: r.data };
    return { ok: false, status: r.status, error: (r.data && r.data.error) || "No se pudo crear" };
}

// PUT /etiquetas/{id} (admin).
export async function renombrarEtiqueta(id, nombre) {
    const r = await authFetch(`${API}/etiquetas/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", ...authHeader() },
        body: JSON.stringify({ nombre }),
        networkAlert: false
    });
    if (r.ok) return { ok: true, etiqueta: r.data };
    return { ok: false, status: r.status, error: (r.data && r.data.error) || "No se pudo guardar" };
}

// DELETE /etiquetas/{id} (admin, 409 si estÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ en uso).
export async function eliminarEtiqueta(id) {
    const r = await authFetch(`${API}/etiquetas/${id}`, {
        method: "DELETE",
        headers: { ...authHeader() }
    });
    return r;
}

// GET /eventos?futuros=true&page= ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â prÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³ximos. Devuelve el Page<> completo con
// content mapeado. En fallo de red devuelve pÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡gina vacÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â­a (igual que hoy: []).
export async function proximos(page = 0) {
    try {
        const params = new URLSearchParams([["futuros", "true"], ["page", page]]);
        const r = await authFetch(`${API}/eventos?${params}`, { networkAlert: false, authFail: "return" });
        if (!r.ok) return paginaVacia();
        return { ...r.data, content: r.data.content.map(apiToView) };
    } catch (e) {
        return paginaVacia();
    }
}

function paginaVacia() {
    return { content: [], totalPages: 1, totalElements: 0, first: true, last: true, number: 0 };
}

// GET /eventos ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â lista completa (recorre todas las pÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ginas; la usa la gestiÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n
// del admin, que muestra todo sin paginar). Devuelve array (igual que hoy).
export async function listarTodos() {
    try {
        const todos = [];
        let page = 0;
        for (;;) {
            const r = await authFetch(`${API}/eventos?page=${page}`, {
                headers: { ...authHeader() },
                networkAlert: false,
                authFail: "return"
            });
            if (!r.ok) return [];
            todos.push(...r.data.content.map(apiToView));
            if (r.data.last) return todos;
            page += 1;
        }
    } catch (e) {
        return [];
    }
}

// POST /eventos o PUT /eventos/{id} multipart (parte "evento" JSON + file/fileHd).
// Devuelve { ok:true, evento } (el backend responde el evento guardado, que en crear
// es la única forma de conocer el id recién generado, necesario para la galería) o
// { ok:false, message } con el texto literal de hoy.
export async function guardar({ dto, file, fileHd, editingId }) {
    const form = new FormData();
    form.append("evento", new Blob([JSON.stringify(dto)], { type: "application/json" }));
    if (file) form.append("file", file);
    if (fileHd) form.append("fileHd", fileHd);
    const r = await authFetch(editingId ? `${API}/eventos/${editingId}` : `${API}/eventos`, {
        method: editingId ? "PUT" : "POST",
        headers: { ...authHeader() },
        body: form
    });
    if (r.ok) return { ok: true, evento: r.data || null };
    return { ok: false, message: "No se pudo guardar: " + ((r.data && r.data.error) || JSON.stringify(r.data) || r.status) };
}

// ---------------------------------------------------------------- 019: galería
// URL de una foto de galería. Vive aquí (y no interpolando ${API} en las
// plantillas) para no exponer la base a los HTML ni repetir el patrón en 7 sitios.
export function galeriaUrl(id, orden) {
    return `${API}/eventos/${id}/galeria/${orden}`;
}

// GET /eventos/{id}/galeria -> [0,2] (índices ocupados). Público y silencioso:
// una foto que falla al listarse no debe romper la tarjeta ni avisar al usuario.
export async function listarIndicesGaleria(id) {
    try {
        const r = await authFetch(`${API}/eventos/${id}/galeria`, { networkAlert: false, authFail: "return" });
        return r.ok && Array.isArray(r.data) ? r.data : [];
    } catch (e) {
        return [];
    }
}

// Rellena ev.galeriaIndices de una lista de eventos ya mapeados a vista. En sitio
// (muta los objetos) para no tener que reasignar la lista en cada composable.
export async function cargarGalerias(evs) {
    await Promise.all((evs || []).map(async ev => {
        ev.galeriaIndices = await listarIndicesGaleria(ev.id);
    }));
    return evs;
}

// POST /eventos/{id}/galeria (multipart: foto + orden). El backend guarda los
// bytes tal cual, sin redimensionar, y responde 201/200 según la posición esté
// libre u ocupada (es un upsert: nunca llega 409 por existir).
export async function subirFotoGaleria(id, orden, file) {
    const form = new FormData();
    form.append("foto", file);
    form.append("orden", String(orden));
    const r = await authFetch(`${API}/eventos/${id}/galeria`, {
        method: "POST",
        headers: { ...authHeader() },
        body: form
    });
    if (r.ok) return { ok: true };
    return { ok: false, message: "No se pudo subir la foto " + (orden + 1) + ": " + ((r.data && r.data.error) || r.status) };
}

// DELETE /eventos/{id}/galeria/{orden} (204). Idempotente en la UI: si el slot
// ya no existe en BD, el borrado ya está hecho y no hay que avisar.
export async function borrarFotoGaleria(id, orden) {
    const r = await authFetch(`${API}/eventos/${id}/galeria/${orden}`, {
        method: "DELETE",
        headers: { ...authHeader() }
    });
    return r.ok;
}

// DELETE /eventos/{id} ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â el llamante refresca siempre (igual que hoy, incluso si !ok).
export async function eliminar(id) {
    const r = await authFetch(`${API}/eventos/${id}`, {
        method: "DELETE",
        headers: { ...authHeader() }
    });
    return r.ok;
}

// GET /eventos/pendientes ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â cola de moderaciÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â³n del admin.
export async function pendientes() {
    const r = await authFetch(`${API}/eventos/pendientes`, { headers: { ...authHeader() } });
    return r.data.map(apiToView);
}

// POST /eventos/{id}/aprobar.
export async function aprobar(id) {
    const r = await authFetch(`${API}/eventos/${id}/aprobar`, {
        method: "POST",
        headers: { ...authHeader() }
    });
    return r.ok ? { ok: true } : { ok: false, message: "No se pudo aprobar." };
}

// POST /eventos/{id}/rechazar { motivo }.
export async function rechazar(id, motivo) {
    const r = await authFetch(`${API}/eventos/${id}/rechazar`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...authHeader() },
        body: JSON.stringify({ motivo: motivo || null })
    });
    return r.ok ? { ok: true } : { ok: false, message: "No se pudo rechazar." };
}

// GET /eventos/mios?page= ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â eventos propios del organizador, en cualquier estado.
// Devuelve el Page<> completo con content mapeado.
export async function mios(page = 0) {
    const r = await authFetch(`${API}/eventos/mios?page=${page}`, { headers: { ...authHeader() } });
    return { ...r.data, content: r.data.content.map(apiToView) };
}

// GET /eventos/favoritos ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â crudo (el llamante mapea a vista y/o a ids).
// loginPage "index.html": mismo literal que verFavoritos/toggleFavorito hoy.
export async function listarFavoritos() {
    const r = await authFetch(`${API}/eventos/favoritos`, {
        headers: { ...authHeader() },
        loginPage: "index.html"
    });
    return r.data;
}

// GET /eventos/favoritos solo-ids y silencioso (si falla, [] igual que hoy).
export async function idsFavoritos() {
    try {
        const r = await authFetch(`${API}/eventos/favoritos`, {
            headers: { ...authHeader() },
            networkAlert: false,
            authFail: "return"
        });
        if (!r.ok) return null; // igual que hoy: se conservan los ya cargados
        return r.data.map(e => e.id);
    } catch (e) {
        return [];
    }
}

// POST /eventos/{id}/favorito.
export async function marcarFavorito(id) {
    const r = await authFetch(`${API}/eventos/${id}/favorito`, {
        method: "POST",
        headers: { ...authHeader() },
        loginPage: "index.html"
    });
    return r.ok;
}

// DELETE /eventos/{id}/favorito.
export async function desmarcarFavorito(id) {
    const r = await authFetch(`${API}/eventos/${id}/favorito`, {
        method: "DELETE",
        headers: { ...authHeader() },
        loginPage: "index.html"
    });
    return r.ok;
}
