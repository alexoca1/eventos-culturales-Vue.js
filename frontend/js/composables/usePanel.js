// usePanel (punto 7: dashboards por rol).
// Sección "Mi panel" con contadores + accesos. Los contadores se leen vía api
// directa (sin efectos laterales en las listas); abrir oculta las demás secciones.
import { ref } from '../../lib/vue.esm-browser.js';
import { store } from '../store.js';
import { proximos, mios, listarTodos, pendientes, listarEtiquetas } from '../api/eventos.js';
import { listarUsuarios } from '../api/auth.js';
import { useEventosInvitado } from './useEventosInvitado.js';
import { useGestionEventos } from './useGestionEventos.js';
import { useModeracion } from './useModeracion.js';
import { useUsuarios } from './useUsuarios.js';
import { useEventoForm } from './useEventoForm.js';
import { useFavoritos } from './useFavoritos.js';
import { useEtiquetas } from './useEtiquetas.js';
import { usePerfil } from './usePerfil.js';
import { useMisEventos } from './useMisEventos.js';

let instance = null;
export function usePanel() {
    if (instance) return instance;

    const showPanel = ref(false);
    // Contadores (se rellenan según el rol al abrir)
    const statProximos = ref(0);
    const statFavs = ref(0);
    const statAprobados = ref(0);
    const statPendientesRev = ref(0);
    const statRechazados = ref(0);
    const statPendientesElim = ref(0);
    const statTotal = ref(0);
    const statPendientes = ref(0);
    const statUsuarios = ref(0);
    const statEtiquetas = ref(0);

    function esAdmin() {
        return store.roles.includes("ROLE_ADMIN");
    }

    function esOrganizador() {
        return store.roles.includes("ROLE_ORGANIZADOR");
    }

    async function abrirPanel() {
        useGestionEventos().showManage.value = false;
        useGestionEventos().showEventSaved.value = false;
        useModeracion().showPendientes.value = false;
        useUsuarios().showUsuarios.value = false;
        useEtiquetas().showEtiquetas.value = false;
        usePerfil().showPerfil.value = false;
        const inv = useEventosInvitado();
        inv.showCover.value = false;
        inv.showEvent.value = false;
        inv.showNoEvent.value = false;
        inv.showProximos.value = false;
        useEventoForm().showForm.value = false;
        useEventoForm().editingId.value = null;
        useFavoritos().showFavoritos.value = false;
        // 019: mismo motivo que en usePerfil: el panel se dibujaba debajo de
        // la lista de "mis eventos" del organizador.
        useMisEventos().showMisEventos.value = false;
        showPanel.value = true;
        try {
            if (esAdmin()) {
                const [todos, pend, usus, tags] = await Promise.all([
                    listarTodos(), pendientes(), listarUsuarios(), listarEtiquetas()
                ]);
                statTotal.value = todos.length;
                statPendientes.value = pend.length;
                statUsuarios.value = usus.usuarios.length;
                statEtiquetas.value = tags.length;
            } else if (esOrganizador()) {
                const [miosEv, prox] = await Promise.all([miosTodas(), proximos()]);
                statAprobados.value = miosEv.filter(e => e.estado === "APROBADO").length;
                statPendientesRev.value = miosEv.filter(e => e.estado === "PENDIENTE_REVISION").length;
                statRechazados.value = miosEv.filter(e => e.estado === "RECHAZADO").length;
                statPendientesElim.value = miosEv.filter(e => e.estado === "PENDIENTE_ELIMINACION").length;
                statProximos.value = prox.totalElements;
            } else {
                const prox = await proximos();
                statProximos.value = prox.totalElements;
                statFavs.value = useFavoritos().favoritosIds.value.length;
            }
        } catch (e) {
            return; // red/401 ya gestionados en api/http.js (alerta + redirección)
        }
    }

    function cerrarPanel() {
        showPanel.value = false;
        useEventosInvitado().showCover.value = true;
    }

    // Junta todas las páginas de Mis eventos (los contadores por estado necesitan el total)
    async function miosTodas() {
        const todas = [];
        let page = 0;
        for (;;) {
            const r = await mios(page);
            todas.push(...r.content);
            if (r.last) return todas;
            page += 1;
        }
    }

    instance = {
        showPanel,
        statProximos, statFavs,
        statAprobados, statPendientesRev, statRechazados, statPendientesElim,
        statTotal, statPendientes, statUsuarios, statEtiquetas,
        abrirPanel, cerrarPanel, esAdmin, esOrganizador
    };
    return instance;
}
