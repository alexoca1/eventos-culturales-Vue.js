// Entrypoint de views/usuarioEstandar.html (F093 de 008-refactor-modular-esm).
// Usa useEventosInvitado, useFavoritos, useLightbox y store (según el Module Map),
// más useAuth (la plantilla necesita tieneSesion/logout: líneas 33-35) y los
// helpers de formato que la plantilla invoca (formatDate/formatRango/formatHorario).
import { createApp, onMounted } from '../../lib/vue.esm-browser.js';
import { store } from '../store.js';
import { useAuth } from '../composables/useAuth.js';
import { useEventosInvitado } from '../composables/useEventosInvitado.js';
import { useFavoritos } from '../composables/useFavoritos.js';
import { useEtiquetas } from '../composables/useEtiquetas.js';
import { usePerfil } from '../composables/usePerfil.js';
import { usePanel } from '../composables/usePanel.js';
import { useVista } from '../composables/useVista.js';
import { useLightbox } from '../composables/useLightbox.js';
import { formatDate, formatRango, formatHorario, textoRed, telHref } from '../ui/format.js';
import { galeriaUrl } from '../api/eventos.js';

const auth = useAuth();
const inv = useEventosInvitado();
const favs = useFavoritos();
const tags = useEtiquetas();
const perfil = usePerfil();
const panel = usePanel();
const vista = useVista();
const lightbox = useLightbox();

createApp({
    setup() {
        onMounted(async () => {
            // 019: <main> no se pinta hasta que listo() abre la puerta, para que
            // al recargar no se vea la portada un instante antes de la cartelera.
            try {
                await auth.cargarSesion(); // primero: renueva el token si expiró
                await tags.cargarEtiquetas(); // catálogo para el filtro multi-etiqueta
                // US2 006: con sesión se cargan los favoritos para cruzarlos en las tarjetas
                if (store.token) {
                    await favs.loadFavoritos();
                }
                // La vista de llegada del invitado es la cartelera de próximos
                // (punto 6); con ?vista= se reabre la que pida la URL.
                if (!await vista.restaurar()) {
                    await inv.cargarProximos();
                }
            } finally {
                vista.listo();
            }
        });
        return {
            ...auth,
            ...inv,
            ...favs,
            ...tags,
            ...perfil,
            ...panel,
            ...lightbox,
            arrancando: vista.arrancando,
formatDate, formatRango, formatHorario, textoRed, telHref,
            galeriaUrl // 019: miniaturas de galería en las tarjetas
        };
    }
}).mount("#eventos");
