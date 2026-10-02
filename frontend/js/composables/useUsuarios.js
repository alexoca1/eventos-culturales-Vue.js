// useUsuarios (F089 de 008-refactor-modular-esm).
// Gestión de usuarios del admin (cargarUsuarios, crearAdmin, editarRolesUsuario,
// toggleActivo...). Mismas firmas y comportamiento que en eventos.js.
// Sin uso en páginas todavía (Fase 5).
import { ref } from '../../lib/vue.esm-browser.js';
import { listarUsuarios, crearAdmin as apiCrearAdmin, actualizarUsuario } from '../api/auth.js';
import { useEventosInvitado } from './useEventosInvitado.js';
import { useGestionEventos } from './useGestionEventos.js';
import { useModeracion } from './useModeracion.js';
import { useEventoForm } from './useEventoForm.js';
import { useEtiquetas } from './useEtiquetas.js';
import { usePerfil } from './usePerfil.js';
import { usePanel } from './usePanel.js';

let instance = null;
export function useUsuarios() {
    if (instance) return instance;

    const showUsuarios = ref(false);
    const usuarios = ref([]); // gestión de usuarios del admin (GET /auth/usuarios)
    const miEmail = ref(""); // email propio (para no auto-bloquearse desde la UI)
    const nuevoAdminEmail = ref("");
    const nuevoAdminPassword = ref("");
    const nuevoAdminNombre = ref("");
    const nuevoAdminApellidos = ref("");
    const nuevoAdminTelefono = ref(""); // punto 4: obligatorio también al crear admin
    const crearAdminError = ref("");
    const editandoUsuarioId = ref(null);
    const editRole = ref(""); // punto 1: rol único (radio), no array
    const editError = ref("");
    // Punto 4: datos de organización (se piden si el radio es ORGANIZADOR)
    const editOrgNombre = ref("");
    const editEncNombre = ref("");
    const editEncTelefono = ref("");
    const editEncEmail = ref("");

    // US1 005: lista de usuarios para el admin
    async function cargarUsuarios() {
        try {
            const r = await listarUsuarios();
            usuarios.value = r.usuarios;
            // Email propio para deshabilitar auto-acciones en la UI (F052)
            miEmail.value = r.miEmail;
        } catch (e) {
            return; // red/401 ya gestionados en api/http.js (alerta + redirección)
        }
        showUsuarios.value = true;
        useEventosInvitado().showProximos.value = false;
        useEtiquetas().showEtiquetas.value = false;
        usePerfil().showPerfil.value = false;
        usePanel().showPanel.value = false;
        const gestion = useGestionEventos();
        gestion.showManage.value = false;
        useModeracion().showPendientes.value = false;
        const inv = useEventosInvitado();
        inv.showCover.value = false;
        inv.showEvent.value = false;
        inv.showNoEvent.value = false;
        useEventoForm().showForm.value = false;
        gestion.showEventSaved.value = false;
        useEventoForm().editingId.value = null;
    }

    // US4 005: crear admin (POST /auth/usuarios-admin ya existente, sin cambios)
    async function crearAdmin() {
        crearAdminError.value = "";
        let r;
        try {
            r = await apiCrearAdmin({
                email: nuevoAdminEmail.value,
                password: nuevoAdminPassword.value,
                nombre: nuevoAdminNombre.value,
                apellidos: nuevoAdminApellidos.value,
                telefono: nuevoAdminTelefono.value
            });
        } catch (e) {
            crearAdminError.value = "No se pudo conectar con el servidor. ¿Está arrancado el backend?";
            return;
        }
        if (!r.ok) {
            crearAdminError.value = r.error || "No se pudo crear";
            return;
        }
        nuevoAdminEmail.value = "";
        nuevoAdminPassword.value = "";
        nuevoAdminNombre.value = "";
        nuevoAdminApellidos.value = "";
        nuevoAdminTelefono.value = "";
        await cargarUsuarios();
    }

    function empezarEditarRoles(u) {
        editandoUsuarioId.value = u.id;
        editRole.value = (u.roles || [])[0] || ""; // legacy multi-rol: preselecciona el primero
        editOrgNombre.value = u.nombreOrganizacion || "";
        editEncNombre.value = u.encargadoNombre || "";
        editEncTelefono.value = u.encargadoTelefono || "";
        editEncEmail.value = u.encargadoEmail || "";
        editError.value = "";
    }

    function cancelarEditarRoles() {
        editandoUsuarioId.value = null;
        editRole.value = "";
        editOrgNombre.value = "";
        editEncNombre.value = "";
        editEncTelefono.value = "";
        editEncEmail.value = "";
        editError.value = "";
    }

    async function editarRolesUsuario(u) {
        const body = { roles: editRole.value ? [editRole.value] : [] };
        if (editRole.value === "ROLE_ORGANIZADOR") {
            body.nombreOrganizacion = editOrgNombre.value;
            body.encargadoNombre = editEncNombre.value;
            body.encargadoTelefono = editEncTelefono.value;
            body.encargadoEmail = editEncEmail.value;
        }
        let r;
        try {
            r = await actualizarUsuario(u.id, body);
        } catch (e) {
            editError.value = "No se pudo conectar con el servidor. ¿Está arrancado el backend?";
            return;
        }
        if (!r.ok) {
            editError.value = r.error || "No se pudo guardar";
            return;
        }
        cancelarEditarRoles();
        await cargarUsuarios();
    }

    function esUnoMismo(u) {
        return miEmail.value !== "" && u.email === miEmail.value;
    }

    // US3 005: activar/desactivar con confirmación; 409 = auto-bloqueo
    async function toggleActivo(u) {
        const accion = u.enabled ? "desactivar" : "activar";
        if (!confirm(`¿${accion} la cuenta de ${u.email}?`)) return;
        let r;
        try {
            r = await actualizarUsuario(u.id, { enabled: !u.enabled });
        } catch (e) {
            return; // red/401 ya gestionados en api/http.js (alerta + redirección)
        }
        if (r.status === 409) {
            alert("No puedes desactivar tu propia cuenta.");
            return;
        }
        if (!r.ok) {
            alert("No se pudo guardar: " + (r.error || r.status));
            return;
        }
        await cargarUsuarios();
    }

    instance = {
        showUsuarios, usuarios, miEmail,
        nuevoAdminEmail, nuevoAdminPassword, nuevoAdminNombre, nuevoAdminApellidos, nuevoAdminTelefono,
        crearAdminError, editandoUsuarioId, editRole, editError,
        editOrgNombre, editEncNombre, editEncTelefono, editEncEmail,
        cargarUsuarios, crearAdmin, empezarEditarRoles, cancelarEditarRoles,
        editarRolesUsuario, esUnoMismo, toggleActivo
    };
    return instance;
}
