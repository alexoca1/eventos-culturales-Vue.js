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
        showEvent: false,
        showForm: false,
        showNoEvent: false,
        showCover: true,
        showManage: false,
        showEventSaved: false,
        editingId: null, // null = crear; con id = editar ese evento
        searchDate: "",
        date: "",
        inputEstablishment: "",
        inputAddress: "",
        inputDate: "",
        inputContent: "",
        inputPoster: "",
        inputMap: "",
        file: null, // cartel display 400px WebP listo para subir (multipart)
        fileHd: null, // cartel HD tope 1600px WebP para el lightbox
        lightboxSrc: "",
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
                poster: `<img src="${API}/eventos/${e.id}/cartel" alt="evento imagen" style="max-width: 400px; max-height: 400px;">`,
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
                window.location.href = "views/administrador.html";
            } catch (e) {
                alert("No se pudo conectar con el servidor. ¿Está arrancado el backend?");
            }
        },
        async searchEvent() {
            if (!this.searchDate) return;
            try {
                const res = await fetch(`${API}/eventos?fecha=${this.searchDate}`);
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
            this.inputContent = "";
            this.inputPoster = "";
            this.file = null;
            this.fileHd = null;
            this.inputMap = "";
            this.showForm = true;
            this.showEvent = false;
            this.showCover = false;
            this.showNoEvent = false;
            this.showManage = false;
            this.showEventSaved = false;
        },
        cancelForm() {
            this.editingId = null;
            this.showForm = false;
        },
        async submitForm() {
            if (this.inputMap && !this.inputMap.includes('google.com/maps/embed')) {
                alert('Por favor, ingresa un código de mapa válido de Google Maps');
                return;
            }

            const dto = {
                establecimiento: this.inputEstablishment,
                direccion: this.inputAddress,
                fecha: this.inputDate,
                descripcion: this.inputContent,
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
                alert("No se pudo guardar: " + (err.error || JSON.stringify(err) || res.status));
                return;
            }

            this.inputEstablishment = "";
            this.inputAddress = "";
            this.inputDate = "";
            this.inputContent = "";
            this.inputPoster = "";
            this.file = null;
            this.fileHd = null;
            this.inputMap = "";
            this.editingId = null;

            this.showForm = false;
            this.showEventSaved = true;
            await this.loadAllEvents();
            if (this.searchDate) await this.searchEvent();
        },
        // Muestra la lista de tarjetas para editar/eliminar
        manageEvents() {
            this.showManage = true;
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
            this.inputContent = ev.content;
            this.inputPoster = ev.poster;
            this.file = null;
            this.fileHd = null;
            this.inputMap = ev.map; // iframe actual precargado (vaciarlo lo borra)
            this.showForm = true;
            this.showManage = false;
            this.showEvent = false;
            this.showCover = false;
            this.showNoEvent = false;
            this.showEventSaved = false;
        },
        async deleteOne(id) {
            if (!confirm("¿Eliminar este evento?")) return;

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
            const estabaGestionando = this.showManage;
            if (this.searchDate) await this.searchEvent();
            this.showManage = estabaGestionando;
        },
        formatDate(date) {
            if (!date) return '';
            const [year, month, day] = date.split('-');
            return `${day}/${month}/${year.slice(2)}`;
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
