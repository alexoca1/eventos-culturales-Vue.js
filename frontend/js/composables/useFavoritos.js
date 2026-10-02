// useFavoritos (F090 de 008-refactor-modular-esm).
// Favoritos del usuario estándar (verFavoritos, volverBuscar, esFavorito,
// toggleFavorito, loadFavoritos). Mismas firmas y comportamiento que en eventos.js.
// Sin uso en páginas todavía (Fase 5).
import { ref } from '../../lib/vue.esm-browser.js';
import { listarFavoritos, idsFavoritos, marcarFavorito, desmarcarFavorito, apiToView, cargarGalerias } from '../api/eventos.js';
import { useEventosInvitado } from './useEventosInvitado.js';
import { usePerfil } from './usePerfil.js';
import { usePanel } from './usePanel.js';

let instance = null;
export function useFavoritos() {
    if (instance) return instance;

    const showFavoritos = ref(false);
    const favoritosIds = ref([]); // IDs favoritos del usuario logueado (para cruzar en tarjetas)
    const favoritosEventos = ref([]); // eventos completos de "Mis favoritos"

    // US4 006: recarga "Mis favoritos" (siempre del servidor: incluye cambios de la sesión)
    async function verFavoritos() {
        let lista;
        try {
            lista = await listarFavoritos();
        } catch (e) {
            return; // red/401 ya gestionados en api/http.js (alerta + redirección)
        }
        favoritosEventos.value = lista.map(e => apiToView(e));
        favoritosIds.value = lista.map(e => e.id);
        // 019: "Mis favoritos" usa tarjetas .mostrar_evento con miniaturas
        await cargarGalerias(favoritosEventos.value);
        showFavoritos.value = true;
        usePerfil().showPerfil.value = false;
        usePanel().showPanel.value = false;
        const inv = useEventosInvitado();
        inv.showEvent.value = false;
        inv.showCover.value = false;
        inv.showNoEvent.value = false;
        inv.showProximos.value = false;
    }

    async function volverBuscar() {
        showFavoritos.value = false;
        usePerfil().showPerfil.value = false;
        usePanel().showPanel.value = false;
        const inv = useEventosInvitado();
        inv.paginaActual.value = 0;
        await inv.cargarProximos(); // punto 6: vuelta a los próximos (frescos a la hora actual)
    }

    // US3 006: estado cruzando el id contra los favoritos ya cargados
    function esFavorito(id) {
        return favoritosIds.value.includes(id);
    }

    // US3 006: sin token → al login; con token → POST/DELETE y actualización local al instante
    async function toggleFavorito(ev) {
        if (!sessionStorage.getItem("token")) {
            window.location.href = "index.html";
            return;
        }
        const esFav = esFavorito(ev.id);
        try {
            const ok = esFav ? await desmarcarFavorito(ev.id) : await marcarFavorito(ev.id);
            if (!ok) return;
        } catch (e) {
            return; // red/401 ya gestionados en api/http.js (alerta + redirección)
        }
        favoritosIds.value = esFav
            ? favoritosIds.value.filter(id => id !== ev.id)
            : [...favoritosIds.value, ev.id];
    }

    // US2 006: IDs favoritos para cruzar en las tarjetas (silencioso: si falla, sin favoritos)
    async function loadFavoritos() {
        const ids = await idsFavoritos();
        if (ids === null) return; // !ok: se conservan los ya cargados (igual que hoy)
        favoritosIds.value = ids;
    }

    instance = {
        showFavoritos, favoritosIds, favoritosEventos,
        verFavoritos, volverBuscar, esFavorito, toggleFavorito, loadFavoritos
    };
    return instance;
}
