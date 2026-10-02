// En producción apunta al backend desplegado (Render). En local/file:// usa localhost.
const RENDER_API = "https://TU-BACKEND.onrender.com";
const API = (location.hostname === "" || location.hostname === "localhost" || location.hostname === "127.0.0.1")
    ? "http://localhost:8081"
    : RENDER_API;

Vue.createApp({
    data() {
        return {
        username: "",
        password: "",
        showRegister: false,
        regEmail: "",
        regPassword: "",
        regNombre: "",
        regApellidos: "",
        regError: "",
        showEvent: false,
        showForm: false,
        showNoEvent: false,
        showCover: true,
        showManage: false,
        showEventSaved: false,
        editingId: null, // null = crear; con id = editar ese evento
        searchDate: "",
        searchCategoria: "",
        date: "",
        inputEstablishment: "",
        inputAddress: "",
        inputDate: "",
        inputFechaFin: "",
        inputHoraInicio: "",
        inputHoraFin: "",
        inputContent: "",
        inputCategoria: "",
        inputPoster: "",
        inputMap: "",
        file: null, // cartel display 400px WebP listo para subir (multipart)
        fileHd: null, // cartel HD tope 1600px WebP para el lightbox
        lightboxSrc: "",
        showPreview: false,
        previewEv: null,
        misEventos: [], // eventos propios del organizador (GET /eventos/mios)
        favoritosIds: [], // IDs favoritos del usuario logueado (para cruzar en tarjetas)
        showFavoritos: false,
        favoritosEventos: [], // eventos completos de "Mis favoritos"
        showUsuarios: false,
        usuarios: [], // gestión de usuarios del admin (GET /auth/usuarios)
        miEmail: "", // email propio (para no auto-bloquearse desde la UI)
        nuevoAdminEmail: "",
        nuevoAdminPassword: "",
        nuevoAdminNombre: "",
        nuevoAdminApellidos: "",
        crearAdminError: "",
        editandoUsuarioId: null,
        editRoles: [],
        editError: "",
        showPendientes: false,
        pendientes: [], // cola de moderación del admin
        rechazoId: null,
        rechazoMotivo: "",
        events: [],      // lista completa (gestión del admin)
        dayEvents: []    // eventos del día buscado (invitado y admin)
        };
    },
    mounted() {
        window.addEventListener("keydown", (e) => { if (e.key === "Escape") this.closeLightbox(); });
        // Guardia del admin: sin token no hay gestión (misma app en las 3 páginas)
        if (window.location.pathname.includes("administrador")) {
            if (!sessionStorage.getItem("token")) {
                window.location.href = "../index.html";
                return;
            }
            this.loadAllEvents();
        }
        // Guardia del organizador: sin token, al login
        if (window.location.pathname.includes("organizador")) {
            if (!sessionStorage.getItem("token")) {
                window.location.href = "../index.html";
                return;
            }
            this.loadMisEventos();
        }
        // US2 006: en la vista de invitado, si hay token se cargan los favoritos
        // para cruzarlos con lo que se muestre (invitado sin login: no hace nada)
        if (window.location.pathname.includes("usuarioEstandar")) {
            if (sessionStorage.getItem("token")) {
                this.loadFavoritos();
            }
        }
    },
    methods: {
        authHeader() {
            return { "Authorization": "Bearer " + sessionStorage.getItem("token") };
        },
        // Clic en el cartel: abre la versión HD en el lightbox
        openLightbox(id) {
            this.lightboxSrc = `${API}/eventos/${id}/cartel-hd`;
        },
        closeLightbox() {
            this.lightboxSrc = "";
        },
        // API (establecimiento, direccion, fecha...) -> vista (establishment, address, date...)
        // El cartel siempre se sirve desde el backend (bytes en BD o redirección a URL externa)
        apiToView(e) {
            return {
                id: e.id,
                establishment: e.establecimiento,
                address: e.direccion,
                date: e.fecha,
                content: e.descripcion,
                categoria: e.categoria || null,
                estado: e.estado || null,
                creadoPor: e.creadoPor ? e.creadoPor.email : null,
                motivoRechazo: e.motivoRechazo || null,
                horaInicio: e.horaInicio || null,
                horaFin: e.horaFin || null,
                fechaFin: e.fechaFin || e.fecha,
                // Sin cartel en BD el src da 404: fallback al banner de portada.
                poster: `<img src="${API}/eventos/${e.id}/cartel" alt="evento imagen" style="max-width: 400px; max-height: 400px;" onerror="this.onerror=null;this.src='../img/evento_portada.jpg'">`,
                map: e.mapaEmbed || ""
            };
        },
        async validateAdmin() {
            try {
                const res = await fetch(`${API}/auth/login`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include", // la cookie de refresh es cross-site en producción
                    body: JSON.stringify({ email: this.username, password: this.password })
                });
                if (!res.ok) {
                    alert("Usuario o contraseña incorrecta, inténtalo de nuevo.");
                    return;
                }
                const data = await res.json();
                sessionStorage.setItem("token", data.accessToken || data.token);
                // US1 004: redirección según rol (el backend devuelve roles como array)
                const roles = (data.user && data.user.roles) || [];
                if (roles.includes("ROLE_ADMIN")) {
                    window.location.href = "views/administrador.html";
                } else if (roles.includes("ROLE_ORGANIZADOR")) {
                    window.location.href = "views/organizador.html";
                } else {
                    // US2 006: usuario estándar → vista de invitado, ahora autenticado
                    window.location.href = "views/usuarioEstandar.html";
                }
            } catch (e) {
                alert("No se pudo conectar con el servidor. ¿Está arrancado el backend?");
            }
        },
        // US1 006: registro + login automático reutilizando validateAdmin()
        async register() {
            this.regError = "";
            let res;
            try {
                res = await fetch(`${API}/auth/register`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        email: this.regEmail,
                        password: this.regPassword,
                        nombre: this.regNombre,
                        apellidos: this.regApellidos
                    })
                });
            } catch (e) {
                this.regError = "No se pudo conectar con el servidor. ¿Está arrancado el backend?";
                return;
            }
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                this.regError = err.error || "No se pudo crear la cuenta";
                return;
            }
            this.username = this.regEmail;
            this.password = this.regPassword;
            this.showRegister = false;
            await this.validateAdmin();
        },
        async searchEvent() {
            if (!this.searchDate && !this.searchCategoria) return;
            try {
                const params = new URLSearchParams();
                if (this.searchDate) params.append("fecha", this.searchDate);
                if (this.searchCategoria) params.append("categoria", this.searchCategoria);
                const res = await fetch(`${API}/eventos?${params}`);
                this.dayEvents = (await res.json()).map(e => this.apiToView(e));
                this.date = this.searchDate;
            } catch (e) {
                alert("No se pudo conectar con el servidor. ¿Está arrancado el backend?");
                return;
            }

            if (this.dayEvents.length > 0) {
                this.showEvent = true;
                this.showCover = false;
                this.showNoEvent = false;
            } else {
                this.showEvent = false;
                this.showCover = false;
                this.showNoEvent = true;
            }

            this.showForm = false;
            this.showManage = false;
            this.showPendientes = false;
            this.showUsuarios = false;
            this.showFavoritos = false;
            this.showEventSaved = false;
            this.editingId = null;
        },
        async loadAllEvents() {
            try {
                const res = await fetch(`${API}/eventos`);
                this.events = (await res.json()).map(e => this.apiToView(e));
            } catch (e) {
                this.events = [];
            }
        },
        // Abre el formulario en modo crear (vacío)
        addEvent() {
            this.editingId = null;
            this.inputEstablishment = "";
            this.inputAddress = "";
            this.inputDate = "";
            this.inputFechaFin = "";
            this.inputHoraInicio = "";
            this.inputHoraFin = "";
            this.inputContent = "";
            this.inputCategoria = "";
            this.inputPoster = "";
            this.file = null;
            this.fileHd = null;
            this.inputMap = "";
            this.showForm = true;
            this.showPreview = false;
            this.previewEv = null;
            this.showEvent = false;
            this.showCover = false;
            this.showNoEvent = false;
            this.showManage = false;
            this.showPendientes = false;
            this.showUsuarios = false;
            this.showEventSaved = false;
        },
        cancelForm() {
            this.editingId = null;
            this.showForm = false;
            this.showPreview = false;
            this.previewEv = null;
        },
        // US2 004: valida y muestra la previsualización sin enviar nada al backend
        previsualizar() {
            if (this.inputMap && !this.inputMap.includes('google.com/maps/embed')) {
                alert('Por favor, ingresa un código de mapa válido de Google Maps');
                return;
            }
            if (this.inputFechaFin && this.inputDate && this.inputFechaFin < this.inputDate) {
                alert('La fecha de fin no puede ser anterior a la de inicio');
                return;
            }
            if (!this.cumple48h()) {
                alert('El evento debe programarse con al menos 48h de antelación');
                return;
            }
            this.previewEv = {
                id: null,
                establishment: this.inputEstablishment,
                address: this.inputAddress,
                date: this.inputDate,
                fechaFin: this.inputFechaFin || this.inputDate,
                content: this.inputContent,
                categoria: this.inputCategoria || null,
                horaInicio: this.inputHoraInicio ? this.inputHoraInicio + ":00" : null,
                horaFin: this.inputHoraFin ? this.inputHoraFin + ":00" : null,
                poster: this.inputPoster,
                map: this.processMap(this.inputMap)
            };
            this.showForm = false;
            this.showCover = false;
            this.showPreview = true;
        },
        volverAEditar() {
            this.showPreview = false;
            this.showForm = true;
        },
        async confirmarEnvio() {
            if (await this.submitForm()) {
                this.showPreview = false;
                this.previewEv = null;
                if (window.location.pathname.includes("organizador")) {
                    await this.loadMisEventos();
                }
            }
        },
        // 48h de antelación con la hora local del navegador (el backend revalida; esto es solo UX)
        cumple48h() {
            if (!this.inputDate) return false;
            const [y, m, d] = this.inputDate.split('-').map(Number);
            const [hh, mm] = (this.inputHoraInicio || "00:00").split(':').map(Number);
            const inicio = new Date(y, m - 1, d, hh, mm);
            return inicio.getTime() - Date.now() >= 48 * 60 * 60 * 1000;
        },
        async submitForm() {
            if (this.inputMap && !this.inputMap.includes('google.com/maps/embed')) {
                alert('Por favor, ingresa un código de mapa válido de Google Maps');
                return;
            }

            if (this.inputFechaFin && this.inputDate && this.inputFechaFin < this.inputDate) {
                alert('La fecha de fin no puede ser anterior a la de inicio');
                return;
            }

            const dto = {
                establecimiento: this.inputEstablishment,
                direccion: this.inputAddress,
                fecha: this.inputDate,
                // Vacía = un día (el backend la normaliza a fecha)
                fechaFin: this.inputFechaFin || null,
                descripcion: this.inputContent,
                // Vacía = null (sin categoría)
                categoria: this.inputCategoria || null,
                // Vacío = null (sin horario); el backend exige ambas o ninguna
                horaInicio: this.inputHoraInicio || null,
                horaFin: this.inputHoraFin || null,
                cartelUrl: "",
                mapaEmbed: this.processMap(this.inputMap)
            };

            const form = new FormData();
            form.append("evento", new Blob([JSON.stringify(dto)], { type: "application/json" }));
            if (this.file) form.append("file", this.file);
            if (this.fileHd) form.append("fileHd", this.fileHd);

            let res;
            const url = this.editingId ? `${API}/eventos/${this.editingId}` : `${API}/eventos`;
            try {
                res = await fetch(url, {
                    method: this.editingId ? "PUT" : "POST",
                    headers: { ...this.authHeader() },
                    body: form
                });
            } catch (e) {
                alert("No se pudo conectar con el servidor. ¿Está arrancado el backend?");
                return false;
            }

            if (res.status === 401 || res.status === 403) {
                alert("Sesión caducada o sin permisos. Vuelve a entrar.");
                sessionStorage.removeItem("token");
                window.location.href = "../index.html";
                return false;
            }
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                alert("No se pudo guardar: " + (err.error || JSON.stringify(err) || res.status));
                return false;
            }

            this.inputEstablishment = "";
            this.inputAddress = "";
            this.inputDate = "";
            this.inputFechaFin = "";
            this.inputHoraInicio = "";
            this.inputHoraFin = "";
            this.inputContent = "";
            this.inputCategoria = "";
            this.inputPoster = "";
            this.file = null;
            this.fileHd = null;
            this.inputMap = "";
            this.editingId = null;

            this.showForm = false;
            this.showPreview = false;
            this.previewEv = null;
            this.showEventSaved = true;
            await this.loadAllEvents();
            if (this.searchDate) await this.searchEvent();
            return true;
        },
        // Muestra la lista de tarjetas para editar/eliminar
        manageEvents() {
            this.showManage = true;
            this.showUsuarios = false;
            this.showPendientes = false;
            this.showCover = false;
            this.showEvent = false;
            this.showNoEvent = false;
            this.showForm = false;
            this.showEventSaved = false;
            this.editingId = null;
            this.loadAllEvents();
        },
        // Precarga el formulario con el evento y entra en modo edición
        editEvento(ev) {
            this.editingId = ev.id;
            this.inputEstablishment = ev.establishment;
            this.inputAddress = ev.address;
            this.inputDate = ev.date;
            // fechaFin null en filas antiguas = un día
            this.inputFechaFin = (ev.fechaFin && ev.fechaFin !== ev.date) ? ev.fechaFin : "";
            // La API devuelve "HH:mm:ss"; el input time usa "HH:mm"
            this.inputHoraInicio = ev.horaInicio ? ev.horaInicio.slice(0, 5) : "";
            this.inputHoraFin = ev.horaFin ? ev.horaFin.slice(0, 5) : "";
            this.inputContent = ev.content;
            this.inputCategoria = ev.categoria || "";
            this.inputPoster = ev.poster;
            this.file = null;
            this.fileHd = null;
            this.inputMap = ev.map; // iframe actual precargado (vaciarlo lo borra)
            this.showForm = true;
            this.showManage = false;
            this.showUsuarios = false;
            this.showEvent = false;
            this.showCover = false;
            this.showNoEvent = false;
            this.showEventSaved = false;
        },
        async deleteOne(id, textoConfirm) {
            if (!confirm(textoConfirm || "¿Eliminar este evento?")) return;

            let res;
            try {
                res = await fetch(`${API}/eventos/${id}`, {
                    method: "DELETE",
                    headers: { ...this.authHeader() }
                });
            } catch (e) {
                alert("No se pudo conectar con el servidor. ¿Está arrancado el backend?");
                return;
            }

            if (res.status === 401 || res.status === 403) {
                alert("Sesión caducada o sin permisos. Vuelve a entrar.");
                sessionStorage.removeItem("token");
                window.location.href = "../index.html";
                return;
            }

            await this.loadAllEvents();
            if (window.location.pathname.includes("organizador")) {
                await this.loadMisEventos();
            }
            const estabaGestionando = this.showManage;
            if (this.searchDate) await this.searchEvent();
            this.showManage = estabaGestionando;
        },
        // US4 004: confirmación con aviso de que requiere aprobación, luego DELETE
        pedirEliminarOrg(id) {
            this.deleteOne(id,
                "¿Solicitar la eliminación de este evento? Esta acción no elimina el evento de inmediato: quedará pendiente hasta que un administrador la apruebe.");
        },
        // US5 004: texto legible del estado para el badge de "Mis eventos"
        textoEstado(estado) {
            return {
                PENDIENTE_REVISION: "Pendiente de revisión",
                APROBADO: "Aprobado",
                RECHAZADO: "Rechazado",
                PENDIENTE_ELIMINACION: "Pendiente de eliminación"
            }[estado] || estado || "";
        },
        // US6 004: etiqueta del tipo de solicitud en la cola de moderación
        tipoSolicitud(estado) {
            if (estado === "PENDIENTE_ELIMINACION") return "Solicitud de eliminación";
            if (estado === "PENDIENTE_REVISION") return "Creación/edición pendiente";
            return this.textoEstado(estado);
        },
        // US6 004: cola de moderación del admin
        async cargarPendientes() {
            try {
                const res = await fetch(`${API}/eventos/pendientes`, { headers: { ...this.authHeader() } });
                if (res.status === 401 || res.status === 403) {
                    alert("Sesión caducada o sin permisos. Vuelve a entrar.");
                    sessionStorage.removeItem("token");
                    window.location.href = "../index.html";
                    return;
                }
                this.pendientes = (await res.json()).map(e => this.apiToView(e));
            } catch (e) {
                alert("No se pudo conectar con el servidor. ¿Está arrancado el backend?");
                return;
            }
            this.showPendientes = true;
            this.showManage = false;
            this.showUsuarios = false;
            this.showCover = false;
            this.showEvent = false;
            this.showNoEvent = false;
            this.showForm = false;
            this.showEventSaved = false;
            this.rechazoId = null;
            this.rechazoMotivo = "";
        },
        async aprobar(id) {
            try {
                const res = await fetch(`${API}/eventos/${id}/aprobar`, {
                    method: "POST",
                    headers: { ...this.authHeader() }
                });
                if (res.status === 401 || res.status === 403) {
                    alert("Sesión caducada o sin permisos. Vuelve a entrar.");
                    sessionStorage.removeItem("token");
                    window.location.href = "../index.html";
                    return;
                }
                if (!res.ok) {
                    alert("No se pudo aprobar.");
                    return;
                }
            } catch (e) {
                alert("No se pudo conectar con el servidor. ¿Está arrancado el backend?");
                return;
            }
            await this.cargarPendientes();
            await this.loadAllEvents();
        },
        pedirRechazo(id) {
            this.rechazoId = id;
            this.rechazoMotivo = "";
        },
        async confirmarRechazo() {
            try {
                const res = await fetch(`${API}/eventos/${this.rechazoId}/rechazar`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json", ...this.authHeader() },
                    body: JSON.stringify({ motivo: this.rechazoMotivo || null })
                });
                if (res.status === 401 || res.status === 403) {
                    alert("Sesión caducada o sin permisos. Vuelve a entrar.");
                    sessionStorage.removeItem("token");
                    window.location.href = "../index.html";
                    return;
                }
                if (!res.ok) {
                    alert("No se pudo rechazar.");
                    return;
                }
            } catch (e) {
                alert("No se pudo conectar con el servidor. ¿Está arrancado el backend?");
                return;
            }
            this.rechazoId = null;
            this.rechazoMotivo = "";
            await this.cargarPendientes();
            await this.loadAllEvents();
        },
        // US1 005: lista de usuarios para el admin
        async cargarUsuarios() {
            try {
                const res = await fetch(`${API}/auth/usuarios`, { headers: { ...this.authHeader() } });
                if (res.status === 401 || res.status === 403) {
                    alert("Sesión caducada o sin permisos. Vuelve a entrar.");
                    sessionStorage.removeItem("token");
                    window.location.href = "../index.html";
                    return;
                }
                this.usuarios = await res.json();
                // Email propio para deshabilitar auto-acciones en la UI (F052)
                try {
                    const perfil = await fetch(`${API}/auth/perfil`, { headers: { ...this.authHeader() } });
                    if (perfil.ok) this.miEmail = (await perfil.json()).email || "";
                } catch (e) {
                    this.miEmail = "";
                }
            } catch (e) {
                alert("No se pudo conectar con el servidor. ¿Está arrancado el backend?");
                return;
            }
            this.showUsuarios = true;
            this.showManage = false;
            this.showPendientes = false;
            this.showCover = false;
            this.showEvent = false;
            this.showNoEvent = false;
            this.showForm = false;
            this.showEventSaved = false;
            this.editingId = null;
        },
        // US4 005: crear admin (POST /auth/usuarios-admin ya existente, sin cambios)
        async crearAdmin() {
            this.crearAdminError = "";
            let res;
            try {
                res = await fetch(`${API}/auth/usuarios-admin`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json", ...this.authHeader() },
                    body: JSON.stringify({
                        email: this.nuevoAdminEmail,
                        password: this.nuevoAdminPassword,
                        nombre: this.nuevoAdminNombre,
                        apellidos: this.nuevoAdminApellidos
                    })
                });
            } catch (e) {
                this.crearAdminError = "No se pudo conectar con el servidor. ¿Está arrancado el backend?";
                return;
            }
            if (res.status === 401 || res.status === 403) {
                alert("Sesión caducada o sin permisos. Vuelve a entrar.");
                sessionStorage.removeItem("token");
                window.location.href = "../index.html";
                return;
            }
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                this.crearAdminError = err.error || "No se pudo crear";
                return;
            }
            this.nuevoAdminEmail = "";
            this.nuevoAdminPassword = "";
            this.nuevoAdminNombre = "";
            this.nuevoAdminApellidos = "";
            await this.cargarUsuarios();
        },
        empezarEditarRoles(u) {
            this.editandoUsuarioId = u.id;
            this.editRoles = [...(u.roles || [])];
            this.editError = "";
        },
        cancelarEditarRoles() {
            this.editandoUsuarioId = null;
            this.editRoles = [];
            this.editError = "";
        },
        async editarRolesUsuario(u) {
            let res;
            try {
                res = await fetch(`${API}/auth/usuarios/${u.id}`, {
                    method: "PUT",
                    headers: { "Content-Type": "application/json", ...this.authHeader() },
                    body: JSON.stringify({ roles: this.editRoles })
                });
            } catch (e) {
                this.editError = "No se pudo conectar con el servidor. ¿Está arrancado el backend?";
                return;
            }
            if (res.status === 401 || res.status === 403) {
                alert("Sesión caducada o sin permisos. Vuelve a entrar.");
                sessionStorage.removeItem("token");
                window.location.href = "../index.html";
                return;
            }
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                this.editError = err.error || "No se pudo guardar";
                return;
            }
            this.cancelarEditarRoles();
            await this.cargarUsuarios();
        },
        esUnoMismo(u) {
            return this.miEmail !== "" && u.email === this.miEmail;
        },
        // US3 005: activar/desactivar con confirmación; 409 = auto-bloqueo
        async toggleActivo(u) {
            const accion = u.enabled ? "desactivar" : "activar";
            if (!confirm(`¿${accion} la cuenta de ${u.email}?`)) return;
            let res;
            try {
                res = await fetch(`${API}/auth/usuarios/${u.id}`, {
                    method: "PUT",
                    headers: { "Content-Type": "application/json", ...this.authHeader() },
                    body: JSON.stringify({ enabled: !u.enabled })
                });
            } catch (e) {
                alert("No se pudo conectar con el servidor. ¿Está arrancado el backend?");
                return;
            }
            if (res.status === 401 || res.status === 403) {
                alert("Sesión caducada o sin permisos. Vuelve a entrar.");
                sessionStorage.removeItem("token");
                window.location.href = "../index.html";
                return;
            }
            if (res.status === 409) {
                alert("No puedes desactivar tu propia cuenta.");
                return;
            }
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                alert("No se pudo guardar: " + (err.error || res.status));
                return;
            }
            await this.cargarUsuarios();
        },
        // US5 006: cerrar sesión sin recargar (vuelve al modo invitado exacto)
        async logout() {
            try {
                await fetch(`${API}/auth/logout`, {
                    method: "POST",
                    credentials: "include" // para que viaje la cookie de refresh a revocar
                });
            } catch (e) {
                // Si no hay red, igual se limpia la sesión local
            }
            sessionStorage.removeItem("token");
            this.favoritosIds = [];
            this.favoritosEventos = [];
            this.showFavoritos = false;
            this.showEvent = false;
            this.showNoEvent = false;
            this.showCover = true;
        },
        // US4 006: hay sesión si queda token (botones y sección de favoritos)
        tieneSesion() {
            return !!sessionStorage.getItem("token");
        },
        // US4 006: recarga "Mis favoritos" (siempre del servidor: incluye cambios de la sesión)
        async verFavoritos() {
            try {
                const res = await fetch(`${API}/eventos/favoritos`, { headers: { ...this.authHeader() } });
                if (res.status === 401 || res.status === 403) {
                    alert("Sesión caducada o sin permisos. Vuelve a entrar.");
                    sessionStorage.removeItem("token");
                    window.location.href = "index.html";
                    return;
                }
                const lista = await res.json();
                this.favoritosEventos = lista.map(e => this.apiToView(e));
                this.favoritosIds = lista.map(e => e.id);
            } catch (e) {
                alert("No se pudo conectar con el servidor. ¿Está arrancado el backend?");
                return;
            }
            this.showFavoritos = true;
            this.showEvent = false;
            this.showCover = false;
            this.showNoEvent = false;
        },
        volverBuscar() {
            this.showFavoritos = false;
            this.showCover = true;
            this.showEvent = false;
            this.showNoEvent = false;
        },
        // US1 005: texto legible de un rol para el badge
        textoRol(rol) {
            return { ROLE_ADMIN: "Admin", ROLE_ORGANIZADOR: "Organizador", ROLE_USER: "Usuario" }[rol] || rol;
        },
        // US5 004: eventos propios en cualquier estado
        async loadMisEventos() {
            try {
                const res = await fetch(`${API}/eventos/mios`, { headers: { ...this.authHeader() } });
                if (res.status === 401 || res.status === 403) {
                    alert("Sesión caducada o sin permisos. Vuelve a entrar.");
                    sessionStorage.removeItem("token");
                    window.location.href = "../index.html";
                    return;
                }
                this.misEventos = (await res.json()).map(e => this.apiToView(e));
            } catch (e) {
                alert("No se pudo conectar con el servidor. ¿Está arrancado el backend?");
            }
        },
        // US3 006: estado cruzando el id contra los favoritos ya cargados
        esFavorito(id) {
            return this.favoritosIds.includes(id);
        },
        // US3 006: sin token → al login; con token → POST/DELETE y actualización local al instante
        async toggleFavorito(ev) {
            if (!sessionStorage.getItem("token")) {
                window.location.href = "index.html";
                return;
            }
            const esFav = this.esFavorito(ev.id);
            try {
                const res = await fetch(`${API}/eventos/${ev.id}/favorito`, {
                    method: esFav ? "DELETE" : "POST",
                    headers: { ...this.authHeader() }
                });
                if (res.status === 401 || res.status === 403) {
                    alert("Sesión caducada o sin permisos. Vuelve a entrar.");
                    sessionStorage.removeItem("token");
                    window.location.href = "index.html";
                    return;
                }
                if (!res.ok) return;
            } catch (e) {
                alert("No se pudo conectar con el servidor. ¿Está arrancado el backend?");
                return;
            }
            this.favoritosIds = esFav
                ? this.favoritosIds.filter(id => id !== ev.id)
                : [...this.favoritosIds, ev.id];
        },
        // US2 006: IDs favoritos para cruzar en las tarjetas (silencioso: si falla, sin favoritos)
        async loadFavoritos() {
            try {
                const res = await fetch(`${API}/eventos/favoritos`, { headers: { ...this.authHeader() } });
                if (!res.ok) return;
                this.favoritosIds = (await res.json()).map(e => e.id);
            } catch (e) {
                this.favoritosIds = [];
            }
        },
        // US3 004: edición del organizador (mismo formulario, precargado)
        editEventoOrg(ev) {
            this.editEvento(ev);
            this.showPreview = false;
            this.previewEv = null;
        },
        formatDate(date) {
            if (!date) return '';
            const [year, month, day] = date.split('-');
            return `${day}/${month}/${year.slice(2)}`;
        },
        // "11/11/26 → 17/11/26" si dura varios días; "" si es de un día
        formatRango(ev) {
            if (!ev.fechaFin || ev.fechaFin === ev.date) return '';
            return `${this.formatDate(ev.date)} → ${this.formatDate(ev.fechaFin)}`;
        },
        // "HH:mm - HH:mm" (+ aviso si cruza medianoche). NO recalcula el cruce:
        // usa fechaFin tal cual lo trae la API (FR-003 frontend 002).
        formatHorario(ev) {
            if (!ev.horaInicio || !ev.horaFin) return '';
            const rango = `${ev.horaInicio.slice(0, 5)} - ${ev.horaFin.slice(0, 5)}`;
            return ev.fechaFin && ev.fechaFin !== ev.date ? rango + " (día siguiente)" : rango;
        },
        processMap(iframe) {
            if (!iframe) return '';
            if (iframe.includes('google.com/maps/embed')) {
                return iframe.replace(/width="[^"]*"/i, 'width="400"')
                    .replace(/height="[^"]*"/i, 'height="300"');
            }
            return iframe;
        },
        // Redimensiona en el navegador a WebP: display 400px + HD tope 1600px.
        // El usuario sube cualquier foto (aunque pese 12 MB) y viajan ~50KB + ~300KB.
        // Solo se rechaza por tipo (los GIF no se admiten); por tamaño, nunca.
        handleFileUpload(event) {
            const file = event.target.files[0];
            if (!file) return;
            const validImageTypes = ['image/jpeg', 'image/png', 'image/webp'];
            if (!validImageTypes.includes(file.type)) {
                alert("Por favor, selecciona una imagen válida (JPEG, PNG o WebP). Los GIF no se admiten.");
                return;
            }
            const img = new Image();
            img.onload = () => {
                URL.revokeObjectURL(img.src);
                this.webify(img, 400, file.name, (f) => {
                    this.file = f;
                    if (f) {
                        const url = URL.createObjectURL(f);
                        this.inputPoster = `<img src="${url}" alt="evento imagen" style="max-width: 400px; max-height: 400px;">`;
                    }
                });
                this.webify(img, 1600, file.name, (f) => { this.fileHd = f; });
            };
            img.src = URL.createObjectURL(file);
        },
        webify(img, maxLado, nombre, listo) {
            const scale = Math.min(1, maxLado / Math.max(img.width, img.height));
            const canvas = document.createElement('canvas');
            canvas.width = Math.round(img.width * scale);
            canvas.height = Math.round(img.height * scale);
            canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height);
            canvas.toBlob((blob) => {
                listo(blob ? new File([blob], nombre.replace(/\.[^.]+$/, "") + ".webp", { type: "image/webp" }) : null);
            }, 'image/webp', 0.85);
        }
    }
}).mount("#eventos");
