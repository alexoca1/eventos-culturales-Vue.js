// SPDX-License-Identifier: MIT

// useAuth (F083 de 008-refactor-modular-esm).
// Envuelve api/auth.js con el estado de la pantalla de login (username, password,
// showRegister, reg*). Mismas firmas y comportamiento que validateAdmin/register/
// logout/tieneSesion en eventos.js. Sin uso en páginas todavía (Fase 5).
import { ref } from '../../lib/vue.esm-browser.js';
import { setSession, clearSession, avisarCierreSesion } from '../store.js';
import { login as apiLogin, register as apiRegister, logout as apiLogout } from '../api/auth.js';
import { authFetch, API, authHeader } from '../api/http.js';
import { useFavoritos } from './useFavoritos.js';
import { useEventosInvitado } from './useEventosInvitado.js';
import { usePerfil } from './usePerfil.js';
import { usePanel } from './usePanel.js';
import { useVista } from './useVista.js';

let instance = null;
export function useAuth() {
    if (instance) return instance;

    const username = ref("");
    const password = ref("");
    const showRegister = ref(false);
    const regEmail = ref("");
    const regPassword = ref("");
    const regNombre = ref("");
    const regApellidos = ref("");
    const regTelefono = ref(""); // punto 4: obligatorio en el registro
    const regError = ref("");
    // 021 T212: consentimiento de privacidad + declaración de 14 años o más.
    // El `required` del input ya frena el submit; el estado se expone por si la
    // plantilla necesita pintarlo.
    const regConsentimiento = ref(false);
    // Punto 5: indicador de sesión del header (nombre visible + dropdown)
    const sesionNombre = ref("");
    const menuAbierto = ref(false);

    async function login() {
        let data;
        try {
            data = await apiLogin(username.value, password.value);
        } catch (e) {
            return; // red caída: ya alertado en api/http.js
        }
        if (!data) {
            alert("Usuario o contraseña incorrecta, inténtalo de nuevo.");
            return;
        }
        const token = data.accessToken || data.token;
        // US1 004: redirección según rol (el backend devuelve roles como array)
        const roles = (data.user && data.user.roles) || [];
        setSession(token, { roles });
        // La plantilla de login también vive en views/login.html: la ruta es
        // relativa al documento, así que el prefijo depende de dónde estemos
        // (con "views/administrador.html" desde views/ salía views/views/).
        const enViews = window.location.pathname.includes('/views/');
        const base = enViews ? '' : 'views/';
        if (roles.includes("ROLE_ADMIN")) {
            window.location.href = base + "administrador.html";
        } else if (roles.includes("ROLE_ORGANIZADOR")) {
            window.location.href = base + "organizador.html";
        } else {
            // US2 006: usuario estándar → vista de invitado, ahora autenticado
            window.location.href = base + "usuarioEstandar.html";
        }
    }

    // US1 006: registro + login automático reutilizando login()
    async function register() {
        regError.value = "";
        let r;
        try {
            r = await apiRegister({
                email: regEmail.value,
                password: regPassword.value,
                nombre: regNombre.value,
                apellidos: regApellidos.value,
                telefono: regTelefono.value
            });
        } catch (e) {
            regError.value = "No se pudo conectar con el servidor. ¿Está arrancado el backend?";
            return;
        }
        if (!r.ok) {
            regError.value = r.error || "No se pudo crear la cuenta";
            return;
        }
        username.value = regEmail.value;
        password.value = regPassword.value;
        showRegister.value = false;
        await login();
    }

    // US5 006 + punto 5: cerrar sesión y volver al inicio para poder reingresar
    async function logout() {
        menuAbierto.value = false;
        await apiLogout();
        clearSession();
        avisarCierreSesion(); // 020: que las demás pestañas también cierren sesión
        const favs = useFavoritos();
        favs.favoritosIds.value = [];
        favs.favoritosEventos.value = [];
        favs.showFavoritos.value = false;
        const inv = useEventosInvitado();
        inv.showEvent.value = false;
        inv.showNoEvent.value = false;
        inv.showCover.value = true;
        window.location.href = "../index.html";
    }

    // US4 006: hay sesión si queda token (botones y sección de favoritos)
    function tieneSesion() {
        return !!sessionStorage.getItem("token");
    }

    // Punto 5: completa el store al cargar una página con sesión persistida
    // (roles/nombre para el indicador del header). Si el access token expiró
    // (401), intenta renovarlo silenciosamente con el refresh de la cookie
    // antes de rendirse; token muerto o red caída → se limpia (igual que hoy).
    async function cargarSesion() {
        const token = sessionStorage.getItem("token");
        if (!token) return;
        let r;
        try {
            r = await authFetch(`${API}/auth/perfil`, {
                headers: { ...authHeader() },
                networkAlert: false,
                authFail: "return"
            });
        } catch (e) {
            clearSession(); // red caída: sin reintento (igual que hoy)
            return;
        }
        if (r.ok && r.data) {
            aplicarPerfil(r.data, token);
            return;
        }
        if (r.status !== 401) {
            clearSession(); // 403 u otro: no es expiración, sin reintento
            return;
        }
        await renovarSesion();
    }

    function aplicarPerfil(data, token) {
        setSession(token, { roles: data.roles || [], miEmail: data.email || "" });
        sesionNombre.value = data.nombreOrganizacion || data.email || "";
    }

    // Renueva el access token con la cookie HttpOnly (el navegador la envía
    // sola con credentials: "include") y recarga el perfil con el token nuevo.
    async function renovarSesion() {
        let rr;
        try {
            rr = await authFetch(`${API}/auth/refresh`, {
                method: "POST",
                credentials: "include",
                networkAlert: false,
                authFail: "return"
            });
        } catch (e) {
            clearSession(); // red caída durante el refresh
            return;
        }
        const nuevoToken = rr.ok && rr.data ? (rr.data.accessToken || rr.data.token) : null;
        const user = rr.ok && rr.data ? rr.data.user : null;
        if (!nuevoToken) {
            clearSession(); // refresh rechazado (401/403): sesión expirada del todo
            return;
        }
        setSession(nuevoToken, { roles: (user && user.roles) || [], miEmail: (user && user.email) || "" });
        try {
            const r2 = await authFetch(`${API}/auth/perfil`, {
                headers: { ...authHeader() },
                networkAlert: false,
                authFail: "return"
            });
            if (r2.ok && r2.data) {
                aplicarPerfil(r2.data, nuevoToken);
            } else {
                // El refresh no trae nombreOrganizacion: degradado al email sin cerrar sesión
                sesionNombre.value = (user && user.email) || "";
            }
        } catch (e2) {
            sesionNombre.value = (user && user.email) || "";
        }
    }

    // 020: una ventana/pestaña nueva arranca sin token en sessionStorage (es por
    // ventana). Si el navegador conserva la cookie de refresh compartida, se
    // recupera la sesión sin pedir credenciales. Silencioso: sin cookie válida
    // o sin backend, el store queda vacío y la página muestra el login.
    async function recuperarSesion() {
        if (sessionStorage.getItem("token")) return;
        await renovarSesion();
    }

    function alternarMenu() {
        menuAbierto.value = !menuAbierto.value;
    }

    // Punto 7: "Mi panel" abre la sección dashboard (sin recargar la página)
    function irAPanel() {
        menuAbierto.value = false;
        usePanel().abrirPanel();
    }

    // Punto 5: "Editar mis datos" desde el dropdown (cierra el menú y abre el perfil)
    function irAPerfil() {
        menuAbierto.value = false;
        usePerfil().abrirPerfil();
    }

    // Punto 7 (019): la cabecera (título) lleva a la vista de llegada del rol.
    // Delega en useVista, que resetea en el sitio si ya estás en tu panel.
    function irAInicio() {
        menuAbierto.value = false;
        useVista().irAInicio();
    }

    // RGPD: borrado de la propia cuenta desde el perfil. El backend
    // (DELETE /auth/perfil) anonimiza email, nombre y teléfono, revoca los tokens
    // y borra los favoritos; los eventos publicados quedan sin vinculación.
    // Aquí se pide confirmación, se limpia la sesión local y se va al inicio con el
    // parámetro que las páginas de acceso leen para mostrar el aviso (paso 4).
    const confirmarBorrado = ref(false);
    const borradoError = ref("");
    const avisoCuentaEliminada = ref(
        new URLSearchParams(window.location.search || "").get("cuentaEliminada") === "1"
    );

    function pedirBorrado() {
        borradoError.value = "";
        confirmarBorrado.value = true;
    }

    function cancelarBorrado() {
        confirmarBorrado.value = false;
    }

    async function eliminarCuenta() {
        borradoError.value = "";
        let r;
        try {
            r = await authFetch(`${API}/auth/perfil`, {
                method: "DELETE",
                headers: { ...authHeader() },
                networkAlert: false,
                authFail: "return"
            });
        } catch (e) {
            borradoError.value = "No se pudo conectar con el servidor. ¿Está arrancado el backend?";
            return;
        }
        if (!r.ok) {
            // 401 = token caducado: misma salida que en guardarPerfil
            if (r.status === 401) {
                await logout();
                return;
            }
            borradoError.value = (r.data && r.data.error) || "No se pudo eliminar la cuenta";
            return;
        }
        clearSession();
        avisarCierreSesion(); // que las demás pestañas se enteren también
        // El botón solo existe en views/, así que el inicio es ../index.html
        window.location.href = "../index.html?cuentaEliminada=1";
    }

    instance = {
        username, password, showRegister,
        regEmail, regPassword, regNombre, regApellidos, regTelefono, regError,
        regConsentimiento,
        sesionNombre, menuAbierto,
        login, register, logout, tieneSesion, cargarSesion, recuperarSesion, alternarMenu,
        // 023: se expone porque al cambiar el email el token viejo deja de identificar
        // al usuario (su `sub` es el email). Quien lo cambie llama a esto para quedarse
        // con un token nuevo sin sacar al usuario de la pantalla.
        renovarSesion,
        // RGPD: borrado de cuenta (confirmación + DELETE) y aviso en el destino
        confirmarBorrado, borradoError, avisoCuentaEliminada,
        pedirBorrado, cancelarBorrado, eliminarCuenta,
        irAPanel, irAPerfil, irAInicio
    };
    return instance;
}
