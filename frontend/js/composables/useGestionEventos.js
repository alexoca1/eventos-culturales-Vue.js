// useGestionEventos (F086 de 008-refactor-modular-esm).
// Lista completa + eliminar (loadAllEvents, manageEvents, deleteOne).
// Mismas firmas y comportamiento que en eventos.js. Sin uso en páginas todavía (Fase 5).
import { ref, computed } from '../../lib/vue.esm-browser.js';
import { listarTodos, eliminar } from '../api/eventos.js';
import { useEventosInvitado } from './useEventosInvitado.js';
import { useEventoForm } from './useEventoForm.js';
import { useUsuarios } from './useUsuarios.js';
import { useModeracion } from './useModeracion.js';
import { useMisEventos } from './useMisEventos.js';
import { useEtiquetas } from './useEtiquetas.js';
import { usePerfil } from './usePerfil.js';
import { usePanel } from './usePanel.js';
let instance = null;
export function useGestionEventos() {
    if (instance) return instance;

    const showManage = ref(false);
    const showEventSaved = ref(false);
    const events = ref([]); // lista completa (gestión del admin)
    // Punto 6: por defecto solo futuros a la fecha y hora de la consulta
    const mostrarAnteriores = ref(false);
    const filtroTexto = ref(""); // 020: filtro de texto en "Gestionar eventos"

    // Fecha local de hoy en ISO (comparación lexicográfica válida en YYYY-MM-DD)
    function hoyISO() {
        const h = new Date();
        const m = String(h.getMonth() + 1).padStart(2, "0");
        const d = String(h.getDate()).padStart(2, "0");
        return `${h.getFullYear()}-${m}-${d}`;
    }

    const eventosGestion = computed(() => {
        const base = mostrarAnteriores.value
            ? events.value
            : events.value.filter(e => (e.fechaFin || e.date) >= hoyISO());
        const q = filtroTexto.value.trim().toLowerCase();
        if (!q) return base;
        // events viene de listarTodos() ya mapeado (apiToView): el lugar se llama
        // `establishment`, no `establecimiento` (ese es el nombre de la API).
        return base.filter(e =>
            (e.nombre || "").toLowerCase().includes(q) ||
            (e.establishment || "").toLowerCase().includes(q));
    });

    async function loadAllEvents() {
        events.value = await listarTodos();
    }

    // Muestra la lista de tarjetas para editar/eliminar
    function manageEvents() {
        showManage.value = true;
        useEventosInvitado().showProximos.value = false;
        useEtiquetas().showEtiquetas.value = false;
        usePerfil().showPerfil.value = false;
        usePanel().showPanel.value = false;
        useUsuarios().showUsuarios.value = false;
        useModeracion().showPendientes.value = false;
        const inv = useEventosInvitado();
        inv.showCover.value = false;
        inv.showEvent.value = false;
        inv.showNoEvent.value = false;
        const form = useEventoForm();
        form.showForm.value = false;
        form.editingId.value = null;
        showEventSaved.value = false;
        // Se devuelve la promesa para que abrir("gestionar") la espere: al
        // restaurar tras un F5 la lista tiene que estar ya en el primer pintado.
        return loadAllEvents();
    }

    async function deleteOne(id, textoConfirm) {
        if (!confirm(textoConfirm || "¿Eliminar este evento?")) return;

        try {
            await eliminar(id);
        } catch (e) {
            return; // red/401 ya gestionados en api/http.js (alerta + redirección)
        }

        await loadAllEvents();
        if (window.location.pathname.includes("organizador")) {
            await useMisEventos().loadMisEventos();
        }
        const estabaGestionando = showManage.value;
        const inv = useEventosInvitado();
        if (inv.searchDate.value) await inv.searchEvent();
        showManage.value = estabaGestionando;
    }

    instance = {
        showManage, showEventSaved, events, mostrarAnteriores, filtroTexto, eventosGestion,
        loadAllEvents, manageEvents, deleteOne
    };
    return instance;
}
