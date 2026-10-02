// useEventosInvitado (F085 de 008-refactor-modular-esm + punto 2: filtro multi-etiqueta).
// Búsqueda pública por fecha/etiquetas (searchEvent) + flags de vista de invitado.
import { ref } from '../../lib/vue.esm-browser.js';
import { buscar, buscarTexto, proximos, cargarGalerias } from '../api/eventos.js';
import { useEventoForm } from './useEventoForm.js';
import { useGestionEventos } from './useGestionEventos.js';
import { useModeracion } from './useModeracion.js';
import { useUsuarios } from './useUsuarios.js';
import { useFavoritos } from './useFavoritos.js';
import { useEtiquetas } from './useEtiquetas.js';
import { usePerfil } from './usePerfil.js';
import { usePanel } from './usePanel.js';

let instance = null;
export function useEventosInvitado() {
    if (instance) return instance;

    const showEvent = ref(false);
    const showCover = ref(true);
    const showNoEvent = ref(false);
    const dayEvents = ref([]); // eventos del día buscado (invitado y admin)
    const searchDate = ref("");
    const searchEtiquetas = ref([]); // punto 2: varias (vacío = sin filtro)
    const searchText = ref(""); // 020: búsqueda por texto (nombre o establecimiento)
    const errBusqueda = ref(""); // 020: error inline si el texto tiene <2 caracteres
    const date = ref("");
    // Punto 6: próximos por defecto (lo que ve el invitado sin buscar)
    const proximosEventos = ref([]);
    const showProximos = ref(false);
    // 017-paginacion: página base 0 + metadatos del Page<> del backend
    const paginaActual = ref(0);
    const paginaMeta = ref({ totalPages: 1, totalElements: 0, first: true, last: true });

    function guardarMeta(pag) {
        paginaMeta.value = {
            totalPages: pag.totalPages,
            totalElements: pag.totalElements,
            first: pag.first,
            last: pag.last
        };
    }

    function desplazarA(id) {
        const el = document.getElementById(id);
        if (el) el.scrollIntoView();
    }

    // Carga los vigentes hoy o después; si no hay ninguno, queda la portada.
    // Apaga el resto de secciones (espejo de searchEvent): solo una visible a la vez.
    async function cargarProximos() {
        // 020: volver a la cartelera de próximos desactiva el buscador de texto.
        searchText.value = "";
        errBusqueda.value = "";
        try {
            const pag = await proximos(paginaActual.value);
            proximosEventos.value = pag.content;
            // 019: los índices de galería no vienen en el listado (una petición por
            // evento). Las tarjetas se pintan igual aunque esta carga falle, así que
            // se espera fuera del try para no vaciar la cartelera.
            await cargarGalerias(proximosEventos.value);
            guardarMeta(pag);
        } catch (e) {
            proximosEventos.value = [];
            paginaMeta.value = { totalPages: 1, totalElements: 0, first: true, last: true };
        }
        useEventoForm().showForm.value = false;
        useEventoForm().editingId.value = null;
        useGestionEventos().showManage.value = false;
        useGestionEventos().showEventSaved.value = false;
        useModeracion().showPendientes.value = false;
        useUsuarios().showUsuarios.value = false;
        useFavoritos().showFavoritos.value = false;
        useEtiquetas().showEtiquetas.value = false;
        usePerfil().showPerfil.value = false;
        usePanel().showPanel.value = false;
        showProximos.value = proximosEventos.value.length > 0;
        showCover.value = proximosEventos.value.length === 0;
        showEvent.value = false;
        showNoEvent.value = false;
    }

    // Entrada a la cartelera desde los botones: siempre desde la página 1.
    function verCartelera() {
        paginaActual.value = 0;
        return cargarProximos();
    }

    async function searchEvent() {
        if (!searchDate.value && !searchEtiquetas.value.length) return;
        try {
            const pag = await buscar({ fecha: searchDate.value, etiquetas: searchEtiquetas.value, page: paginaActual.value });
            dayEvents.value = pag.content;
            // 019: igual que en cargarProximos(), la galería no puede tumbar la búsqueda
            await cargarGalerias(dayEvents.value);
            guardarMeta(pag);
            date.value = searchDate.value;
        } catch (e) {
            return; // red/401 ya gestionados en api/http.js (alerta + redirección)
        }

        if (dayEvents.value.length > 0) {
            showEvent.value = true;
            showCover.value = false;
            showNoEvent.value = false;
        } else {
            showEvent.value = false;
            showCover.value = false;
            showNoEvent.value = true;
        }

        useEventoForm().showForm.value = false;
        useEventoForm().editingId.value = null;
        useGestionEventos().showManage.value = false;
        useGestionEventos().showEventSaved.value = false;
        useModeracion().showPendientes.value = false;
        useUsuarios().showUsuarios.value = false;
        useFavoritos().showFavoritos.value = false;
        useEtiquetas().showEtiquetas.value = false;
        usePerfil().showPerfil.value = false;
        usePanel().showPanel.value = false;
        showProximos.value = false;
    }

    // 020: busca por texto (FR-002). Valida >=2 caracteres, limpia fecha/etiquetas
    // (FR-004) y pinta los resultados. `page` permite reutilizarla desde la
    // paginación; las llamadas del botón/Enter (sin argumento) son una búsqueda
    // nueva → página 0.
    async function buscarPorTexto(page = 0) {
        const q = searchText.value.trim();
        if (q.length < 2) {
            errBusqueda.value = "Escribe al menos 2 caracteres";
            return;
        }
        errBusqueda.value = "";
        searchDate.value = "";
        searchEtiquetas.value = [];
        date.value = "";
        paginaActual.value = page;
        try {
            const pag = await buscarTexto({ q, page });
            dayEvents.value = pag.content;
            // 019: igual que en searchEvent(), la galería no puede tumbar la búsqueda
            await cargarGalerias(dayEvents.value);
            guardarMeta(pag);
        } catch (e) {
            return; // red/401 ya gestionados en api/http.js (alerta + redirección)
        }

        if (dayEvents.value.length > 0) {
            showEvent.value = true;
            showCover.value = false;
            showNoEvent.value = false;
        } else {
            showEvent.value = false;
            showCover.value = false;
            showNoEvent.value = true;
        }

        useEventoForm().showForm.value = false;
        useEventoForm().editingId.value = null;
        useGestionEventos().showManage.value = false;
        useGestionEventos().showEventSaved.value = false;
        useModeracion().showPendientes.value = false;
        useUsuarios().showUsuarios.value = false;
        useFavoritos().showFavoritos.value = false;
        useEtiquetas().showEtiquetas.value = false;
        usePerfil().showPerfil.value = false;
        usePanel().showPanel.value = false;
        showProximos.value = false;
    }

    // Reinicia a página 0 al cambiar filtros (nunca pedir una página que ya no existe)
    function filtrar() {
        paginaActual.value = 0;
        searchText.value = ""; // 020: fecha/etiquetas desactivan el buscador de texto (FR-004)
        return searchEvent();
    }

    // 019-atajos: fecha local en YYYY-MM-DD. toISOString() daría la fecha en UTC,
    // que entre medianoche y las 2h (UTC+1/+2) es el día anterior: "Hoy" buscaría
    // ayer y "Próxima semana" caería un día antes. Mismo formato que el hoyISO()
    // de useGestionEventos.js:25.
    function aISO(d) {
        const m = String(d.getMonth() + 1).padStart(2, "0");
        const dia = String(d.getDate()).padStart(2, "0");
        return `${d.getFullYear()}-${m}-${dia}`;
    }

    // Atajos del buscador: fijan la fecha y buscan, como el @change del input.
    function hoy() {
        searchDate.value = aISO(new Date());
        return filtrar();
    }

    function finDeSemana() {
        const d = new Date();
        const diasHastaSabado = (6 - d.getDay() + 7) % 7;
        d.setDate(d.getDate() + diasHastaSabado);
        searchDate.value = aISO(d);
        return filtrar();
    }

    function proximaSemana() {
        const d = new Date();
        d.setDate(d.getDate() + 7);
        searchDate.value = aISO(d);
        return filtrar();
    }

    async function paginaSiguiente() {
        if (showProximos.value) {
            paginaActual.value += 1;
            await cargarProximos();
            desplazarA("lista-proximos");
        } else if (searchDate.value || searchEtiquetas.value.length) {
            paginaActual.value += 1;
            await searchEvent();
            desplazarA("lista-eventos");
        } else if (searchText.value.trim().length >= 2) {
            paginaActual.value += 1;
            await buscarPorTexto(paginaActual.value);
            desplazarA("lista-eventos");
        }
    }

    async function paginaAnterior() {
        if (paginaActual.value === 0) return;
        if (showProximos.value) {
            paginaActual.value -= 1;
            await cargarProximos();
            desplazarA("lista-proximos");
        } else if (searchDate.value || searchEtiquetas.value.length) {
            paginaActual.value -= 1;
            await searchEvent();
            desplazarA("lista-eventos");
        } else if (searchText.value.trim().length >= 2) {
            paginaActual.value -= 1;
            await buscarPorTexto(paginaActual.value);
            desplazarA("lista-eventos");
        }
    }

    // 019: bajo demanda, para cuando una tarjeta llegue sin índices (p. ej. relogueo
    // con la lista ya en memoria). Si ya se cargaron, no repite la petición.
    function cargarGaleria(ev) {
        if (!ev || (ev.galeriaIndices && ev.galeriaIndices.length)) return Promise.resolve(ev);
        return cargarGalerias([ev]);
    }

    instance = {
        showEvent, showCover, showNoEvent, dayEvents, searchDate, searchEtiquetas, searchText,
        errBusqueda, date,
        proximosEventos, showProximos,
        paginaActual, paginaMeta, paginaSiguiente, paginaAnterior, filtrar,
        hoy, finDeSemana, proximaSemana,
        buscarPorTexto, searchEvent, cargarProximos, verCartelera, cargarGaleria
    };
    return instance;
}
