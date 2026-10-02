// Entrypoint de views/organizador.html (F095 de 008-refactor-modular-esm).
// Usa useEventoForm, useMisEventos y useLightbox (según el Module Map), más
// useEventosInvitado (showCover de la plantilla: línea 36) y useGestionEventos
// (showEventSaved: línea 114), y los helpers que la plantilla invoca
// (format*, textoEstado, handleFileUpload).
import { createApp, onMounted } from '../../lib/vue.esm-browser.js';
import { store, esOrganizador } from '../store.js';
import { useAuth } from '../composables/useAuth.js';
import { useEventoForm } from '../composables/useEventoForm.js';
import { useEventosInvitado } from '../composables/useEventosInvitado.js';
import { useGestionEventos } from '../composables/useGestionEventos.js';
import { useMisEventos } from '../composables/useMisEventos.js';
import { useEtiquetas } from '../composables/useEtiquetas.js';
import { usePerfil } from '../composables/usePerfil.js';
import { usePanel } from '../composables/usePanel.js';
import { useVista } from '../composables/useVista.js';
import { useLightbox } from '../composables/useLightbox.js';
import { formatDate, formatRango, formatHorario, textoEstado, textoRed, telHref } from '../ui/format.js';
import { handleFileUpload as uiHandleFileUpload } from '../ui/imagenes.js';
import { galeriaUrl } from '../api/eventos.js';

const form = useEventoForm();
const auth = useAuth();
const inv = useEventosInvitado();
const gestion = useGestionEventos();
const mis = useMisEventos();
const tags = useEtiquetas();
const perfil = usePerfil();
const panel = usePanel();
const vista = useVista();
const lightbox = useLightbox();

// Ver admin.js: mismo adaptador estado-plano -> refs del formulario.
const uploadState = {
    set file(v) { form.file.value = v; },
    get file() { return form.file.value; },
    set fileHd(v) { form.fileHd.value = v; },
    get fileHd() { return form.fileHd.value; },
    set inputPoster(v) { form.inputPoster.value = v; },
    get inputPoster() { return form.inputPoster.value; }
};
function handleFileUpload(e) {
    uiHandleFileUpload(e, uploadState);
}

// 020-buscador-texto: al lanzar una búsqueda desde el panel se cierran las
// secciones propias del organizador (mis eventos/formulario) para que se vean
// los resultados que pinta useEventosInvitado (showEvent/dayEvents).
function buscarPublico(accion) {
    mis.showMisEventos.value = false;
    form.showForm.value = false;
    form.showPreview.value = false;
    return accion();
}

createApp({
    setup() {
        onMounted(async () => {
            // 019: <main> no se pinta hasta que listo() abre la puerta, para que
            // al recargar no se vea la portada un instante antes de la sección.
            try {
                // Guardia del organizador: sin token, al login (igual que en eventos.js)
                if (!store.token) {
                    window.location.href = "../index.html";
                    return;
                }
                await auth.cargarSesion(); // primero: renueva el token si expiró
                if (!store.token) {
                    window.location.href = "../index.html"; // sesión muerta tras el refresh: al login
                    return;
                }
                // 020: el token puede ser de otro rol (se cerró sesión y se entró
                // como admin): esta página no debe mostrar la sesión anterior.
                if (!esOrganizador.value) {
                    window.location.href = "../index.html";
                    return;
                }
                await tags.cargarEtiquetas(); // catálogo para los checkboxes del formulario
                // La vista de llegada del organizador es "Mis eventos": con ?vista=
                // se reabre la que pida la URL (su opener ya carga lo suyo).
                if (!await vista.restaurar()) {
                    await mis.loadMisEventos();
                }
            } finally {
                vista.listo();
            }
        });
        // 020: al volver con "Atrás" el navegador puede restaurar esta página desde
        // su caché (bfcache) con la sesión de antes; se recarga para reevaluar la
        // guardia de rol con el token actual.
        window.addEventListener("pageshow", (e) => { if (e.persisted) location.reload(); });
        return {
            ...auth,
            ...form,
            ...inv,
            ...gestion,
            ...mis,
            ...tags,
            ...perfil,
            ...panel,
            ...lightbox,
            arrancando: vista.arrancando,
            formatDate, formatRango, formatHorario, textoEstado, textoRed, telHref,
            handleFileUpload,
            buscarPublico,
            galeriaUrl // 019: miniaturas de galería en las tarjetas
        };
    }
}).mount("#eventos");
