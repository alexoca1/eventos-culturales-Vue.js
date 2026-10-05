// usePerfil (punto 4: cada usuario edita sus propios datos).
// Lee de GET /auth/perfil (vía listarUsuarios, que ya lo trae) y guarda con
// PUT /auth/perfil. Los datos de organización solo aplican si es ORGANIZADOR.
// 023: el email también se edita. Es el `sub` del JWT, así que al cambiarlo hay que
// renovar la sesión (useAuth().renovarSesion()) o el token viejo deja de identificar.
import { ref } from '../../lib/vue.esm-browser.js';
import { actualizarPerfil } from '../api/auth.js';
import { authFetch, API, authHeader } from '../api/http.js';
import { useAuth } from './useAuth.js';
import { useEventosInvitado } from './useEventosInvitado.js';
import { useGestionEventos } from './useGestionEventos.js';
import { useModeracion } from './useModeracion.js';
import { useUsuarios } from './useUsuarios.js';
import { useEventoForm } from './useEventoForm.js';
import { useFavoritos } from './useFavoritos.js';
import { useEtiquetas } from './useEtiquetas.js';
import { useMisEventos } from './useMisEventos.js';
import { usePanel } from './usePanel.js';

let instance = null;
export function usePerfil() {
    if (instance) return instance;

    const showPerfil = ref(false);
    const perfilEmail = ref("");
    // 023: lo que hay en la BD, para distinguir "guardé el mismo email" de "lo cambié".
    // Sin esto, cada guardado gastaría un refresh de la cookie.
    const emailGuardado = ref("");
    const perfilNombre = ref("");
    const perfilApellidos = ref("");
    const perfilTelefono = ref("");
    const perfilOrgNombre = ref("");
    const perfilEncNombre = ref("");
    const perfilEncTelefono = ref("");
    const perfilEncEmail = ref("");
    const perfilError = ref("");
    const perfilOk = ref(false);

    // Rellena desde el perfil ya cargado (lo trae listarUsuarios) o recargándolo
    async function abrirPerfil(perfil) {
        perfilError.value = "";
        perfilOk.value = false;
        if (perfil) {
            rellenar(perfil);
        } else {
            try {
                const r = await authFetch(`${API}/auth/perfil`, {
                    headers: { ...authHeader() },
                    networkAlert: false,
                    authFail: "return"
                });
                if (r.ok && r.data) rellenar(r.data);
            } catch (e) {
                perfilError.value = "No se pudo conectar con el servidor. ¿Está arrancado el backend?";
                return;
            }
        }
        showPerfil.value = true;
        useEventosInvitado().showProximos.value = false;
        usePanel().showPanel.value = false;
        useGestionEventos().showManage.value = false;
        useGestionEventos().showEventSaved.value = false;
        useModeracion().showPendientes.value = false;
        useUsuarios().showUsuarios.value = false;
        useEtiquetas().showEtiquetas.value = false;
        const inv = useEventosInvitado();
        inv.showCover.value = false;
        inv.showEvent.value = false;
        inv.showNoEvent.value = false;
        useEventoForm().showForm.value = false;
        useEventoForm().editingId.value = null;
        useFavoritos().showFavoritos.value = false;
        // 019: "mis eventos" es la lista del organizador; si no se apaga aquí
        // el perfil se dibuja debajo de la lista.
        useMisEventos().showMisEventos.value = false;
    }

    function rellenar(p) {
        perfilEmail.value = p.email || "";
        emailGuardado.value = perfilEmail.value;
        perfilNombre.value = p.nombre || "";
        perfilApellidos.value = p.apellidos || "";
        perfilTelefono.value = p.telefono || "";
        perfilOrgNombre.value = p.nombreOrganizacion || "";
        perfilEncNombre.value = p.encargadoNombre || "";
        perfilEncTelefono.value = p.encargadoTelefono || "";
        perfilEncEmail.value = p.encargadoEmail || "";
    }

    async function guardarPerfil(esOrg) {
        perfilError.value = "";
        perfilOk.value = false;
        const body = {
            email: perfilEmail.value,
            nombre: perfilNombre.value,
            apellidos: perfilApellidos.value,
            telefono: perfilTelefono.value
        };
        if (esOrg) {
            body.nombreOrganizacion = perfilOrgNombre.value;
            body.encargadoNombre = perfilEncNombre.value;
            body.encargadoTelefono = perfilEncTelefono.value;
            body.encargadoEmail = perfilEncEmail.value;
        }
        let r;
        try {
            r = await actualizarPerfil(body);
        } catch (e) {
            perfilError.value = "No se pudo conectar con el servidor. ¿Está arrancado el backend?";
            return;
        }
        if (!r.ok) {
            // 401 = token caducado de verdad (actualizarPerfil ya no redirige, para poder
            // enseñar el 403 de la demo): cerramos sesión como hacía http.js por defecto.
            if (r.status === 401) {
                await useAuth().logout();
                return;
            }
            perfilError.value = r.error || "No se pudo guardar";
            return;
        }
        perfilOk.value = true;
        // 023: el token que llevamos tiene el email viejo en su `sub`, así que ya no
        // identifica a nadie. renovarSesion() lo cambia por uno nuevo con la cookie
        // de refresh (que sigue viva) y actualiza el nombre/email de la cabecera.
        // Si el refresh falla, useAuth limpia la sesión y el usuario vuelve al login.
        if (r.perfil && r.perfil.email && r.perfil.email !== emailGuardado.value) {
            emailGuardado.value = r.perfil.email;
            await useAuth().renovarSesion();
        }
    }

    function cerrarPerfil() {
        showPerfil.value = false;
    }

    instance = {
        showPerfil, perfilEmail, perfilNombre, perfilApellidos, perfilTelefono,
        perfilOrgNombre, perfilEncNombre, perfilEncTelefono, perfilEncEmail,
        perfilError, perfilOk,
        abrirPerfil, guardarPerfil, cerrarPerfil
    };
    return instance;
}
