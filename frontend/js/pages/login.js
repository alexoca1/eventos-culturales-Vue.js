// Entrypoint de index.html (F092 de 008-refactor-modular-esm).
// Solo usa useAuth (según el Module Map). Sin mounted: esta página no tiene
// guardia ni precarga (igual que en eventos.js).
import { createApp, onMounted, ref } from '../../lib/vue.esm-browser.js';
import { store } from '../store.js';
import { useAuth } from '../composables/useAuth.js';

const auth = useAuth();

createApp({
    setup() {
        // Con sesión válida no hay nada que loguear: rebote al panel del rol
        // (patrón estándar; evita la ilusión de "aparezco no logueado").
        // Sin token, o con token muerto (cargarSesion lo limpia), se queda aquí.
        // 020: mientras se comprueba si hay sesión recuperable, el formulario de
        // acceso permanece oculto (evita verlo un instante antes de rebotar al
        // panel cuando la cookie de refresh permite recuperar la sesión).
        const comprobando = ref(true);
        async function comprobarSesion() {
            comprobando.value = true;
            if (sessionStorage.getItem("token")) {
                await auth.cargarSesion(); // token propio: valida/renueva
            } else {
                await auth.recuperarSesion(); // ventana nueva: cookie de refresh
            }
            comprobando.value = false;
            if (!store.token) return;
            // Esta plantilla también vive en views/login.html: las rutas son
            // relativas al documento, así que el prefijo depende de dónde estemos.
            const enViews = window.location.pathname.includes('/views/');
            const base = enViews ? '' : 'views/';
            if (store.roles.includes("ROLE_ADMIN")) {
                window.location.href = base + "administrador.html";
            } else if (store.roles.includes("ROLE_ORGANIZADOR")) {
                window.location.href = base + "organizador.html";
            } else {
                window.location.href = base + "usuarioEstandar.html";
            }
        }
        onMounted(comprobarSesion);
        // 020: al volver con "Atrás", el navegador puede restaurar esta página desde
        // su caché (bfcache) sin reejecutar el módulo: se ve el login viejo "sin
        // sesión" aunque el token siga vivo. En esa restauración (pageshow) se
        // repite la comprobación para rebotar al panel.
        window.addEventListener("pageshow", (e) => { if (e.persisted) comprobarSesion(); });
        return {
            ...auth,
            comprobando,
            // Sin token el invitado ve el login como hoy; con token el formulario
            // queda oculto desde el primer pintado (evita su flash antes del rebote).
            hayToken: !!sessionStorage.getItem("token"),
            // La plantilla llama "validateAdmin" al login (nombre original del método)
            validateAdmin: auth.login,
            // La portada de esta página es estática (showCover nunca cambia aquí)
            showCover: true
        };
    }
}).mount("#eventos");
