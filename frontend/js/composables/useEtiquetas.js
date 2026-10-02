// useEtiquetas (punto 2: catálogo + CRUD admin de etiquetas).
// Estado del catálogo (etiquetasDisponibles) para formularios y filtros,
// más la gestión del admin (crear/renombrar/eliminar).
import { ref } from '../../lib/vue.esm-browser.js';
import { listarEtiquetas, crearEtiqueta, renombrarEtiqueta, eliminarEtiqueta } from '../api/eventos.js';
import { useGestionEventos } from './useGestionEventos.js';
import { useModeracion } from './useModeracion.js';
import { useUsuarios } from './useUsuarios.js';
import { useEventoForm } from './useEventoForm.js';
import { useEventosInvitado } from './useEventosInvitado.js';
import { usePerfil } from './usePerfil.js';
import { usePanel } from './usePanel.js';

let instance = null;
export function useEtiquetas() {
    if (instance) return instance;

    const etiquetasDisponibles = ref([]); // catálogo ({id, nombre}) para formularios y filtros
    const showEtiquetas = ref(false);
    const nuevaEtiquetaNombre = ref("");
    const etiquetaError = ref("");
    const editandoEtiquetaId = ref(null);
    const editEtiquetaNombre = ref("");

    async function cargarEtiquetas() {
        etiquetasDisponibles.value = await listarEtiquetas();
    }

    // Sección de gestión del admin (muestra la lista y abre el panel)
    async function gestionarEtiquetas() {
        await cargarEtiquetas();
        showEtiquetas.value = true;
        useEventosInvitado().showProximos.value = false;
        usePerfil().showPerfil.value = false;
        usePanel().showPanel.value = false;
        useGestionEventos().showManage.value = false;
        useGestionEventos().showEventSaved.value = false;
        useModeracion().showPendientes.value = false;
        useUsuarios().showUsuarios.value = false;
        const inv = useEventosInvitado();
        inv.showCover.value = false;
        inv.showEvent.value = false;
        inv.showNoEvent.value = false;
        useEventoForm().showForm.value = false;
        useEventoForm().editingId.value = null;
    }

    async function crear() {
        etiquetaError.value = "";
        let r;
        try {
            r = await crearEtiqueta(nuevaEtiquetaNombre.value);
        } catch (e) {
            etiquetaError.value = "No se pudo conectar con el servidor. ¿Está arrancado el backend?";
            return;
        }
        if (!r.ok) {
            etiquetaError.value = r.error || "No se pudo crear";
            return;
        }
        nuevaEtiquetaNombre.value = "";
        await cargarEtiquetas();
    }

    function empezarRenombrar(t) {
        editandoEtiquetaId.value = t.id;
        editEtiquetaNombre.value = t.nombre;
        etiquetaError.value = "";
    }

    function cancelarRenombrar() {
        editandoEtiquetaId.value = null;
        editEtiquetaNombre.value = "";
        etiquetaError.value = "";
    }

    async function confirmarRenombrar(t) {
        let r;
        try {
            r = await renombrarEtiqueta(t.id, editEtiquetaNombre.value);
        } catch (e) {
            etiquetaError.value = "No se pudo conectar con el servidor. ¿Está arrancado el backend?";
            return;
        }
        if (!r.ok) {
            etiquetaError.value = r.error || "No se pudo guardar";
            return;
        }
        cancelarRenombrar();
        await cargarEtiquetas();
    }

    async function eliminar(t) {
        if (!confirm(`¿Eliminar la etiqueta "${t.nombre}"?`)) return;
        let r;
        try {
            r = await eliminarEtiqueta(t.id);
        } catch (e) {
            return; // red/401 ya gestionados en api/http.js (alerta + redirección)
        }
        if (r.status === 409) {
            alert("No se puede eliminar: hay eventos con esta etiqueta.");
            return;
        }
        if (!r.ok) {
            const d = r.data || {};
            alert("No se pudo eliminar: " + (d.error || r.status));
            return;
        }
        await cargarEtiquetas();
    }

    instance = {
        etiquetasDisponibles, showEtiquetas, nuevaEtiquetaNombre, etiquetaError,
        editandoEtiquetaId, editEtiquetaNombre,
        cargarEtiquetas, gestionarEtiquetas, crear,
        empezarRenombrar, cancelarRenombrar, confirmarRenombrar, eliminar
    };
    return instance;
}
