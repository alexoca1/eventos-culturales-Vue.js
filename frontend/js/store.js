// Estado de sesión reactivo y único (Fase 1 de 008-refactor-modular-esm).
// Sustituye las lecturas directas de sessionStorage.getItem("token") repartidas
// por eventos.js: sessionStorage queda solo como persistencia (leída una vez al
// importar este módulo), y el resto del código usa este store como punto de verdad.
// Todavía sin uso en ninguna página (ver tasks.md) — solo existe, verificable con node --check.
import { reactive, computed } from '../lib/vue.esm-browser.js';

export const store = reactive({
    token: sessionStorage.getItem("token") || "",
    roles: [],
    miEmail: ""
});

export const esAdmin = computed(() => store.roles.includes("ROLE_ADMIN"));
export const esOrganizador = computed(() => store.roles.includes("ROLE_ORGANIZADOR"));

// Guarda la sesión tras un login OK (equivale al sessionStorage.setItem actual).
export function setSession(token, { roles = [], miEmail = "" } = {}) {
    store.token = token || "";
    store.roles = roles;
    store.miEmail = miEmail;
    if (token) {
        sessionStorage.setItem("token", token);
    } else {
        sessionStorage.removeItem("token");
    }
}

// Limpia la sesión (logout o 401/403). Equivale al sessionStorage.removeItem actual.
export function clearSession() {
    setSession("", { roles: [], miEmail: "" });
}

// 020: coherencia de sesión entre pestañas. El token de acceso vive en
// sessionStorage (una copia por pestaña), pero la sesión se comparte por la
// cookie de refresh; si se cierra sesión en una pestaña, las demás seguirían
// "logueadas" con su token hasta que caducara. El evento "storage" (localStorage)
// se emite en las OTRAS pestañas del mismo origen; la que cierra sesión navega
// por su cuenta. Por localStorage solo viaja la señal, nunca el token.
const CLAVE_LOGOUT = "sesion:logout";

export function avisarCierreSesion() {
    try {
        localStorage.setItem(CLAVE_LOGOUT, String(Date.now()));
    } catch (e) {
        // localStorage bloqueado (modo privado): sin aviso cross-tab, no es crítico
    }
}

if (typeof window !== "undefined" && typeof window.addEventListener === "function") {
    window.addEventListener("storage", (e) => {
        if (e.key !== CLAVE_LOGOUT) return;
        clearSession();
        window.location.reload();
    });
}
