// useMisEventos (F087 de 008-refactor-modular-esm).
// Eventos propios del organizador (loadMisEventos, pedirEliminarOrg, editEventoOrg).
// Mismas firmas y comportamiento que en eventos.js. Sin uso en páginas todavía (Fase 5).
import { ref } from '../../lib/vue.esm-browser.js';
import { mios } from '../api/eventos.js';
import { useGestionEventos } from './useGestionEventos.js';
import { useEventoForm } from './useEventoForm.js';
import { useEventosInvitado } from './useEventosInvitado.js';

let instance = null;
export function useMisEventos() {
    if (instance) return instance;

    const misEventos = ref([]); // eventos propios del organizador (GET /eventos/mios)
    // 019: nace en false como el resto de secciones. En true contaminaba la
    // vista de las otras dos páginas (que no muestran esta lista) y hacía que
    // la URL de admin/organizador apuntase a "mis-eventos" sin serlo.
    const showMisEventos = ref(false);
    // 017-paginacion: página base 0 + metadatos del Page<> del backend
    const misPaginaActual = ref(0);
    const misPaginaMeta = ref({ totalPages: 1, totalElements: 0, first: true, last: true });

    function desplazarA(id) {
        const el = document.getElementById(id);
        if (el) el.scrollIntoView();
    }

    // US5 004: eventos propios en cualquier estado
    async function loadMisEventos() {
        try {
            const pag = await mios(misPaginaActual.value);
            misEventos.value = pag.content;
            misPaginaMeta.value = {
                totalPages: pag.totalPages,
                totalElements: pag.totalElements,
                first: pag.first,
                last: pag.last
            };
            showMisEventos.value = true;
            // 019: la portada se apaga cuando hay lista; si no hay eventos
            // propios la portada sigue siendo lo que ve el organizador.
            useEventosInvitado().showCover.value = misEventos.value.length === 0;
        } catch (e) {
            return; // red/401 ya gestionados en api/http.js (alerta + redirección)
        }
    }

    // US4 004: confirmación con aviso de que requiere aprobación, luego DELETE
    function pedirEliminarOrg(id) {
        useGestionEventos().deleteOne(id,
            "¿Solicitar la eliminación de este evento? Esta acción no elimina el evento de inmediato: quedará pendiente hasta que un administrador la apruebe.");
    }

    // US3 004: edición del organizador (mismo formulario, precargado)
    function editEventoOrg(ev) {
        const form = useEventoForm();
        form.editEvento(ev);
        form.showPreview.value = false;
        form.previewEv.value = null;
    }

    async function misPaginaSiguiente() {
        misPaginaActual.value += 1;
        await loadMisEventos();
        desplazarA("lista-mis-eventos");
    }

    async function misPaginaAnterior() {
        if (misPaginaActual.value === 0) return;
        misPaginaActual.value -= 1;
        await loadMisEventos();
        desplazarA("lista-mis-eventos");
    }

    instance = {
        misEventos, showMisEventos,
        misPaginaActual, misPaginaMeta, misPaginaSiguiente, misPaginaAnterior,
        loadMisEventos, pedirEliminarOrg, editEventoOrg
    };
    return instance;
}
