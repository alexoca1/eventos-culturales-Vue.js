// useVista (019-navegacion-directa-seccion-persistente).
// Da nombre a la sección visible y lo refleja en la URL (?vista=<nombre>), de
// modo que recargar el navegador devuelve al usuario a donde estaba. También
// centraliza "ver la portada", que hasta ahora cada opener repetía a mano.
import { computed, nextTick, ref, watch } from '../../lib/vue.esm-browser.js';
import { store } from '../store.js';
import { VISTAS, LANDING, PANEL_POR_ROL } from '../ui/vistas.js';
import { useEventosInvitado } from './useEventosInvitado.js';
import { useEventoForm } from './useEventoForm.js';
import { useGestionEventos } from './useGestionEventos.js';
import { useModeracion } from './useModeracion.js';
import { useUsuarios } from './useUsuarios.js';
import { useMisEventos } from './useMisEventos.js';
import { useEtiquetas } from './useEtiquetas.js';
import { usePerfil } from './usePerfil.js';
import { useFavoritos } from './useFavoritos.js';
import { usePanel } from './usePanel.js';

let instance = null;
export function useVista() {
    if (instance) return instance;

    const inv = useEventosInvitado();

    // Puerta de arranque. Mientras esté cerrada las plantillas no pintan <main>:
    // la sesión (auth.cargarSesion) y la restauración son async, así que sin esto
    // se veía la portada un instante antes de la sección pedida. La abre el
    // propio mounted de cada página con listo(), y el temporizador de seguridad
    // evita que un fetch colgado deje la página en blanco para siempre.
    const arrancando = ref(true);
    const seguroArranque = setTimeout(() => { arrancando.value = false; }, 5000);
    seguroArranque.unref?.();

    function listo() {
        clearTimeout(seguroArranque);
        arrancando.value = false;
    }

    // Estado de la búsqueda (fecha + etiquetas): sobrevive al recargar porque no
    // cabe en ?vista=. Se guarda en sessionStorage y no en la URL para no
    // ensuciarla con filtros; la URL solo dice QUÉ sección se ve.
    const CLAVE_BUSQUEDA = "vista:busqueda";
    watch(
        [() => inv.searchDate.value, () => inv.searchEtiquetas.value.join("|")],
        () => sessionStorage.setItem(CLAVE_BUSQUEDA, JSON.stringify({
            fecha: inv.searchDate.value,
            etiquetas: inv.searchEtiquetas.value
        }))
    );
    function busquedaGuardada() {
        try {
            return JSON.parse(sessionStorage.getItem(CLAVE_BUSQUEDA));
        } catch {
            return null;
        }
    }
    // Reaplicar una búsqueda sin filtros guardados no puede hacer nada
    // (searchEvent sale sin hacer nada): en ese caso se cae a la portada.
    function abrirBusqueda() {
        const b = busquedaGuardada();
        if (!b || (!b.fecha && !(b.etiquetas || []).length)) return verPortada();
        inv.searchDate.value = b.fecha || "";
        inv.searchEtiquetas.value = b.etiquetas || [];
        return inv.searchEvent();
    }

    // Una entrada por sección: si está visible y cómo se reabre. El orden de
    // VISTAS (ui/vistas.js) es el que decide cuál gana si varias coexistieran.
    let secciones = null;
    function sec() {
        if (secciones) return secciones;
        const form = useEventoForm();
        const gest = useGestionEventos();
        const mod = useModeracion();
        const us = useUsuarios();
        const mis = useMisEventos();
        const tags = useEtiquetas();
        const perfil = usePerfil();
        const favs = useFavoritos();
        const panel = usePanel();
        secciones = {
            pendientes: {
                visible: () => mod.showPendientes.value,
                abrir: () => mod.cargarPendientes()
            },
            usuarios: {
                visible: () => us.showUsuarios.value,
                abrir: () => us.cargarUsuarios()
            },
            etiquetas: {
                visible: () => tags.showEtiquetas.value,
                abrir: () => tags.gestionarEtiquetas()
            },
            gestionar: {
                visible: () => gest.showManage.value,
                abrir: () => gest.manageEvents()
            },
            favoritos: {
                visible: () => favs.showFavoritos.value,
                abrir: () => favs.verFavoritos()
            },
            perfil: {
                visible: () => perfil.showPerfil.value,
                abrir: () => perfil.abrirPerfil()
            },
            panel: {
                visible: () => panel.showPanel.value,
                abrir: () => panel.abrirPanel()
            },
            formulario: {
                visible: () => form.showForm.value,
                abrir: () => form.addEvent()
            },
            "mis-eventos": {
                visible: () => mis.showMisEventos.value,
                abrir: () => mis.loadMisEventos()
            },
            proximos: {
                visible: () => inv.showProximos.value,
                abrir: () => inv.verCartelera()
            },
            busqueda: {
                visible: () => inv.showEvent.value,
                abrir: () => abrirBusqueda()
            },
            "sin-resultados": {
                visible: () => inv.showNoEvent.value,
                abrir: () => abrirBusqueda()
            },
            portada: {
                visible: () => inv.showCover.value,
                abrir: () => verPortada()
            }
        };
        return secciones;
    }

    // Vista visible ahora mismo, o "" si solo hay estados transitorios
    // (previsualización o banner de "guardado"), que no se guardan en la URL.
    const vistaActual = computed(() => {
        const s = sec();
        for (const nombre of VISTAS) {
            if (s[nombre].visible()) return nombre;
        }
        return "";
    });

    // La URL es la memoria de la vista y una entrada de historial por sección:
    // pushState deja que "atrás" vuelva a la sección anterior en vez de saltar a
    // la URL pelada. `desdeHistorial` marca los cambios que vienen del propio
    // historial (restaurar/popstate): ahí la entrada ya existe y solo se corrige,
    // así que se usa replaceState para no duplicarla.
    let desdeHistorial = false;
    function escribirUrl(v) {
        if (!v) return;
        const u = new URL(window.location.href);
        if (u.searchParams.get("vista") === v) return;
        // La entrada de llegada (URL pelada) no es una navegación: la sustituye
        // el landing en el sitio, no añade un paso nuevo.
        const inicial = !u.searchParams.has("vista");
        u.searchParams.set("vista", v);
        const metodo = (desdeHistorial || inicial) ? "replaceState" : "pushState";
        window.history[metodo](null, "", u);
    }
    watch(vistaActual, escribirUrl);

    // Nombre de la página actual: "administrador" | "organizador" | ...
    function paginaActual() {
        const archivo = window.location.pathname.split("/").pop() || "";
        return archivo.replace(".html", "");
    }

    function rolActual() {
        if (store.roles.includes("ROLE_ADMIN")) return "ROLE_ADMIN";
        if (store.roles.includes("ROLE_ORGANIZADOR")) return "ROLE_ORGANIZADOR";
        return "";
    }

    // Apaga todas las secciones y deja solo la portada.
    function verPortada() {
        const form = useEventoForm();
        const gest = useGestionEventos();
        useModeracion().showPendientes.value = false;
        useUsuarios().showUsuarios.value = false;
        useEtiquetas().showEtiquetas.value = false;
        useFavoritos().showFavoritos.value = false;
        usePerfil().showPerfil.value = false;
        usePanel().showPanel.value = false;
        useMisEventos().showMisEventos.value = false;
        gest.showManage.value = false;
        gest.showEventSaved.value = false;
        form.showForm.value = false;
        form.showPreview.value = false;
        form.previewEv.value = null;
        form.editingId.value = null;
        inv.showCover.value = true;
        inv.showEvent.value = false;
        inv.showNoEvent.value = false;
        inv.showProximos.value = false;
    }

    // Reabre una sección por nombre. Nombre desconocido -> la vista de
    // llegada de esta página (así una URL manipulada nunca deja la página
    // vacía ni salta a un panel que no corresponde).
    function abrir(nombre) {
        const s = sec();
        if (s[nombre]) return s[nombre].abrir();
        const llegada = LANDING[paginaActual()];
        if (llegada && s[llegada]) return s[llegada].abrir();
        return verPortada();
    }

    // Al montar: si la URL trae ?vista=, se reabre esa sección. Sin el
    // parámetro la página se queda en su vista de llegada (comportamiento
    // actual, sin peticiones extra). Devuelve si hubo que restaurar, para que
    // la página pueda saltarse su carga base si la vista restaurada ya la hizo.
    async function restaurar() {
        const pedida = new URL(window.location.href).searchParams.get("vista");
        if (!pedida) return false;
        desdeHistorial = true;
        try {
            await abrir(pedida);
            // Reajusta la URL: si el nombre no era restaurable (p. ej. una búsqueda
            // sin filtros guardados) la vista real ya no es la que pedía la URL.
            escribirUrl(vistaActual.value);
            await nextTick();
        } finally {
            desdeHistorial = false;
        }
        return true;
    }

    // Atrás/Adelante: popstate trae la URL de la entrada destino. Se reabre su
    // sección (sin parámetro -> LANDING de la página) sin empujar una entrada
    // nueva, porque ya estamos sobre ella.
    async function aplicarHistorial() {
        desdeHistorial = true;
        try {
            const pedida = new URL(window.location.href).searchParams.get("vista");
            await abrir(pedida);
            await nextTick();
        } finally {
            desdeHistorial = false;
        }
    }
    window.addEventListener("popstate", aplicarHistorial);

    // Cabecera: "ir a mi panel". Si ya estoy en la página de mi rol reseteo en
    // el sitio (sin navegación). Antes pasaba por index.html, que pintaba el
    // login entero antes de rebotar: de ahí el parpadeo.
    // El invitado es la excepción: no tiene panel al que volver, así que su
    // cabecera es el regreso a index.html. Sin este caso, LANDING[usuarioEstandar]
    // le devolvía a la cartelera en el sitio y el clic no llevaba a ninguna parte.
    function irAInicio() {
        if (!store.token) {
            window.location.assign("../index.html");
            return;
        }
        const pagina = paginaActual();
        if (Object.hasOwn(LANDING, pagina)) {
            abrir(LANDING[pagina]);
            return;
        }
        window.location.assign(PANEL_POR_ROL[rolActual()] || "../index.html");
    }

    instance = {
        vistaActual, paginaActual, arrancando,
        abrir, verPortada, restaurar, irAInicio, listo
    };
    return instance;
}
