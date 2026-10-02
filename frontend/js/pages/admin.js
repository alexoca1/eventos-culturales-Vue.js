// Entrypoint de views/administrador.html (F094 de 008-refactor-modular-esm).
// Usa useEventoForm, useGestionEventos, useModeracion, useUsuarios y useLightbox
// (según el Module Map), más useEventosInvitado (la plantilla trae el mismo
// buscador de invitado: líneas 18-22, 39-66) y los helpers que la plantilla
// invoca (format*, textoRol, tipoSolicitud, handleFileUpload).
import { createApp, onMounted } from '../../lib/vue.esm-browser.js';
import { store, esAdmin } from '../store.js';
import { useAuth } from '../composables/useAuth.js';
import { useEventoForm } from '../composables/useEventoForm.js';
import { useEventosInvitado } from '../composables/useEventosInvitado.js';
import { useGestionEventos } from '../composables/useGestionEventos.js';
import { useModeracion } from '../composables/useModeracion.js';
import { useUsuarios } from '../composables/useUsuarios.js';
import { useEtiquetas } from '../composables/useEtiquetas.js';
import { usePerfil } from '../composables/usePerfil.js';
import { usePanel } from '../composables/usePanel.js';
import { useVista } from '../composables/useVista.js';
import { useLightbox } from '../composables/useLightbox.js';
import { formatDate, formatRango, formatHorario, tipoSolicitud, textoRol, textoRed, telHref } from '../ui/format.js';
import { handleFileUpload as uiHandleFileUpload } from '../ui/imagenes.js';
import { galeriaUrl } from '../api/eventos.js';

const form = useEventoForm();
const auth = useAuth();
const inv = useEventosInvitado();
const gestion = useGestionEventos();
const mod = useModeracion();
const users = useUsuarios();
const tags = useEtiquetas();
const perfil = usePerfil();
const panel = usePanel();
const vista = useVista();
const lightbox = useLightbox();

// ui/imagenes.js escribe en un objeto de estado plano; este adaptador lo conecta
// a los refs del formulario (asignar el ref entero rompería la reactividad).
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

createApp({
    setup() {
        onMounted(async () => {
            // 019: <main> no se pinta hasta que listo() abre la puerta, para que
            // al recargar no se vea la portada un instante antes de la sección.
            try {
                // Guardia del admin: sin token no hay gestión (igual que en eventos.js)
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
                // como organizador): esta página no debe mostrar la sesión anterior.
                if (!esAdmin.value) {
                    window.location.href = "../index.html";
                    return;
                }
                await tags.cargarEtiquetas(); // catálogo para los checkboxes del filtro
                // Si la URL trae ?vista=gestionar (u otra sección), se reabre y su
                // opener carga sus propios datos. Sin parámetro la vista de
                // llegada del admin es la portada, que no necesita ninguna petición.
                await vista.restaurar();
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
            ...inv,
            ...form,
            ...gestion,
            ...mod,
            ...users,
            ...tags,
            ...perfil,
            ...panel,
            ...lightbox,
            arrancando: vista.arrancando,
formatDate, formatRango, formatHorario, tipoSolicitud, textoRol, textoRed, telHref,
            handleFileUpload,
            galeriaUrl // 019: miniaturas de galería en las tarjetas
        };
    }
}).mount("#eventos");
