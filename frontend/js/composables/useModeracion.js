// useModeracion (F088 de 008-refactor-modular-esm).
// Cola de moderación del admin (cargarPendientes, aprobar, pedirRechazo, confirmarRechazo).
// Mismas firmas y comportamiento que en eventos.js. Sin uso en páginas todavía (Fase 5).
import { ref } from '../../lib/vue.esm-browser.js';
import { pendientes as apiPendientes, aprobar as apiAprobar, rechazar as apiRechazar } from '../api/eventos.js';
import { useEventosInvitado } from './useEventosInvitado.js';
import { useGestionEventos } from './useGestionEventos.js';
import { useUsuarios } from './useUsuarios.js';
import { useEventoForm } from './useEventoForm.js';
import { useEtiquetas } from './useEtiquetas.js';
import { usePerfil } from './usePerfil.js';
import { usePanel } from './usePanel.js';

let instance = null;
export function useModeracion() {
    if (instance) return instance;

    const showPendientes = ref(false);
    const pendientes = ref([]); // cola de moderación del admin
    const rechazoId = ref(null);
    const rechazoMotivo = ref("");

    // US6 004: cola de moderación del admin
    async function cargarPendientes() {
        try {
            pendientes.value = await apiPendientes();
        } catch (e) {
            return; // red/401 ya gestionados en api/http.js (alerta + redirección)
        }
        showPendientes.value = true;
        useEventosInvitado().showProximos.value = false;
        useEtiquetas().showEtiquetas.value = false;
        usePerfil().showPerfil.value = false;
        usePanel().showPanel.value = false;
        const gestion = useGestionEventos();
        gestion.showManage.value = false;
        useUsuarios().showUsuarios.value = false;
        const inv = useEventosInvitado();
        inv.showCover.value = false;
        inv.showEvent.value = false;
        inv.showNoEvent.value = false;
        useEventoForm().showForm.value = false;
        gestion.showEventSaved.value = false;
        rechazoId.value = null;
        rechazoMotivo.value = "";
    }

    async function aprobar(id) {
        let r;
        try {
            r = await apiAprobar(id);
        } catch (e) {
            return; // red/401 ya gestionados en api/http.js (alerta + redirección)
        }
        if (!r.ok) {
            alert(r.message);
            return;
        }
        await cargarPendientes();
        await useGestionEventos().loadAllEvents();
    }

    function pedirRechazo(id) {
        rechazoId.value = id;
        rechazoMotivo.value = "";
    }

    async function confirmarRechazo() {
        let r;
        try {
            r = await apiRechazar(rechazoId.value, rechazoMotivo.value);
        } catch (e) {
            return; // red/401 ya gestionados en api/http.js (alerta + redirección)
        }
        if (!r.ok) {
            alert(r.message);
            return;
        }
        rechazoId.value = null;
        rechazoMotivo.value = "";
        await cargarPendientes();
        await useGestionEventos().loadAllEvents();
    }

    instance = {
        showPendientes, pendientes, rechazoId, rechazoMotivo,
        cargarPendientes, aprobar, pedirRechazo, confirmarRechazo
    };
    return instance;
}
