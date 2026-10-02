// useEventoForm (F084 de 008-refactor-modular-esm + 019-nombre-descripcion-galeria).
// Formulario compartido crear/editar + previsualización del organizador.
import { ref } from '../../lib/vue.esm-browser.js';
import {
    guardar, galeriaUrl, listarIndicesGaleria, subirFotoGaleria, borrarFotoGaleria
} from '../api/eventos.js';
import { processMap } from '../ui/mapa.js';
import { webify, esperarCompresionCartel } from '../ui/imagenes.js';
import { useEventosInvitado } from './useEventosInvitado.js';
import { useGestionEventos } from './useGestionEventos.js';
import { useModeracion } from './useModeracion.js';
import { useUsuarios } from './useUsuarios.js';
import { useFavoritos } from './useFavoritos.js';
import { useMisEventos } from './useMisEventos.js';
import { useEtiquetas } from './useEtiquetas.js';
import { usePerfil } from './usePerfil.js';
import { usePanel } from './usePanel.js';

// 019: el backend fija MAX_FOTOS = 5 y los órdenes son 0..4. Se recorre en las
// 5 plantillas con v-for="orden in galeriaSlots".
const galeriaSlots = [0, 1, 2, 3, 4];

// El backend limita cada petición a 2 MB (spring.servlet.multipart.max-request-size),
// pero las fotos de galería se muestran en el lightbox, así que se comprimen al
// mismo lado que el cartel HD: 1600px. Los tipos son los que acepta el backend.
const TIPOS_FOTO = ["image/jpeg", "image/png", "image/webp"];
const LADO_FOTO_GALERIA = 1600;

// Qué hay que hacer con cada posición al guardar. Se exporta suelto para poder
// comprobar la tabla sin navegador (ver js/ui/galeria.check.js). Los tres estados
// de `fotos[orden]` son distintos y NO se pueden colapsar:
//   { file, preview } -> hay archivo nuevo: subirlo (POST; el backend hace upsert)
//   null              -> el usuario pulsó "x" en este slot: borrarlo si existía
//   undefined         -> slot sin tocar: nada (¡o se perdería la foto al guardar!)
export function planGaleria(ordenes, fotos, existentes) {
    return ordenes
        .map(orden => {
            if (fotos[orden]) return { orden, accion: "subir" };
            // Solo el null explícito cuenta como "quitada". Con el array vacío
            // (fotos = []) todos los órdenes dan undefined, y mirar solo
            // `existentes` borraba la galería entera al editar y guardar.
            if (fotos[orden] === null) return { orden, accion: existentes.includes(orden) ? "borrar" : "nada" };
            return { orden, accion: "nada" };
        })
        .filter(p => p.accion !== "nada");
}

let instance = null;
export function useEventoForm() {
    if (instance) return instance;

    const editingId = ref(null); // null = crear; con id = editar ese evento
    const inputEstablishment = ref("");
    const inputAddress = ref("");
    const inputDate = ref("");
    const inputFechaFin = ref("");
    const inputHoraInicio = ref("");
    const inputHoraFin = ref("");
    // 019: `nombre` es el título corto y obligatorio (lo que se ve en la tarjeta);
    // `descripcion` es el texto largo y opcional. Antes ambos iban en inputContent.
    const inputNombre = ref("");
    const inputDescripcion = ref("");
    // 019 (F190): contacto y redes del evento. inputRedes = [{red, url}], máximo
    // 8 (lo impone el botón "Añadir red social" al llegar a 8).
    const inputRedes = ref([]);
    const inputTelefono = ref("");
    const inputUrlEvento = ref("");
    const inputEtiquetas = ref([]); // punto 2: varias (vacío = OTROS por defecto en backend)
    const inputPoster = ref("");
    const inputMap = ref("");
    const file = ref(null); // cartel display 400px WebP listo para subir (multipart)
    const fileHd = ref(null); // cartel HD tope 1600px WebP para el lightbox
    // 019: archivos nuevos por posición (hueco = slot vacío) y los órdenes que ya
    // tienen foto guardada, para distinguir "quitar" de "nunca hubo".
    const fotosGaleria = ref([]);
    const galeriaExistente = ref([]);
    const showForm = ref(false);
    const showPreview = ref(false);
    const previewEv = ref(null);
    // Punto 3: errores por campo (mensaje rojo bajo el input + foco al primero)
    const errEstablishment = ref("");
    const errAddress = ref("");
    const errDate = ref("");
    const errNombre = ref("");
    const errCartel = ref("");

    // 019: compresiones de foto en vuelo. `esperarCompresionGaleria()` las agota
    // antes de guardar para no subir el archivo sin comprimir.
    const procesandoGaleria = new Set();

    // 019: liberas los objectURL de las previsualizaciones al vaciar el formulario
    function liberarPreviewsGaleria() {
        for (const orden of Object.keys(fotosGaleria.value)) {
            const previa = fotosGaleria.value[orden] && fotosGaleria.value[orden].preview;
            if (previa) URL.revokeObjectURL(previa);
        }
        fotosGaleria.value = [];
        galeriaExistente.value = [];
    }

    // --- 019: estado de un slot para la plantilla ---------------------------
    function slotOcupado(orden) {
        return Boolean(fotosGaleria.value[orden]) || galeriaExistente.value.includes(orden);
    }

    // Al pulsar "x" en una foto YA guardada la miniatura sigue visible (no se borra
    // hasta guardar, ver planGaleria), así que sin esto el clic no daba ninguna
    // señal y parecía no funcionar.
    function slotPendienteBorrar(orden) {
        return fotosGaleria.value[orden] === null && galeriaExistente.value.includes(orden);
    }

    // Miniatura del slot: la previsualización local si hay archivo nuevo, y si no
    // la foto ya guardada (misma URL que usan las miniaturas de las tarjetas).
    function slotPreview(orden) {
        const nueva = fotosGaleria.value[orden];
        if (nueva) return nueva.preview;
        return galeriaExistente.value.includes(orden) ? galeriaUrl(editingId.value, orden) : "";
    }

    function limpiarErrores() {
        errEstablishment.value = "";
        errAddress.value = "";
        errDate.value = "";
        errNombre.value = "";
        errCartel.value = "";
    }

    // 019 (F190): añade una fila de red con valores por defecto (FACEBOOK, URL vacía).
    // El usuario la rellena; el backend valida la URL al guardar.
    function añadirRed() {
        inputRedes.value.push({ red: "FACEBOOK", url: "" });
    }

    function quitarRed(i) {
        inputRedes.value.splice(i, 1);
    }

    // Valida los obligatorios; pone el mensaje bajo cada input que falte y
    // lleva el foco al primero. En crear el cartel es obligatorio (punto 3);
    // en edición se conserva el existente y no se exige.
    function validarObligatorios() {
        limpiarErrores();
        let primero = null;
        if (!inputEstablishment.value) {
            errEstablishment.value = "Indica el establecimiento donde se celebra el evento.";
            primero = primero || "f-establecimiento";
        }
        if (!inputAddress.value) {
            errAddress.value = "Indica la dirección del evento.";
            primero = primero || "f-direccion";
        }
        if (!inputDate.value) {
            errDate.value = "Elige la fecha de inicio del evento.";
            primero = primero || "f-fecha";
        }
        if (!inputNombre.value) {
            errNombre.value = "Escribe el nombre del evento.";
            primero = primero || "f-nombre";
        }
        if (editingId.value === null && !file.value) {
            errCartel.value = "Sube la imagen del cartel (obligatoria al crear).";
            primero = primero || "fileInput";
        }
        if (primero) {
            document.getElementById(primero)?.focus();
            return false;
        }
        return true;
    }

    // 019:handlers de los 5 slots de galería.
    //
    // La foto se comprime a WebP ANTES de subirla, igual que el cartel. Sin esto,
    // una foto de móvil (3-8 MB) superaba `spring.servlet.multipart.max-request-size`
    // (2 MB), Tomcat reseteba la conexión, `fetch` rechazaba y el alert decía
    // "no se pudo conectar con el servidor" aunque el evento sí se guardaba.
    function elegirFotoGaleria(orden, event) {
        const f = event.target.files && event.target.files[0];
        if (!f) return;
        if (!TIPOS_FOTO.includes(f.type)) {
            alert("Selecciona una imagen válida (JPEG, PNG o WebP). Los GIF no se admiten.");
            event.target.value = "";
            return;
        }
        if (fotosGaleria.value[orden]) URL.revokeObjectURL(fotosGaleria.value[orden].preview);
        // Es Object.assign y no reasignar fotosGaleria.value: la plantilla hace
        // v-for sobre el array y una reasignación descartaría el resto de slots.
        // El preview sale ya con el original para que no haya espera visual.
        Object.assign(fotosGaleria.value, { [orden]: { file: f, preview: URL.createObjectURL(f) } });

        const conversion = new Promise((hecho) => {
            const img = new Image();
            img.onload = () => {
                URL.revokeObjectURL(img.src);
                webify(img, LADO_FOTO_GALERIA, f.name, (comprimida) => {
                    const slot = fotosGaleria.value[orden];
                    // Mientras comprimía, el usuario pudo cambiar la foto o vaciar el slot.
                    if (comprimida && slot && slot.file === f) {
                        URL.revokeObjectURL(slot.preview);
                        Object.assign(fotosGaleria.value, {
                            [orden]: { file: comprimida, preview: URL.createObjectURL(comprimida) }
                        });
                    }
                    // Si webify falla (toBlob a null) se queda el original: mejor
                    // intentar y que el backend lo mida que perder la selección.
                    hecho();
                });
            };
            img.onerror = () => hecho();   // fichero corrupto: no se bloquea el guardado
            img.src = URL.createObjectURL(f);
        });
        procesandoGaleria.add(conversion);
        conversion.then(() => procesandoGaleria.delete(conversion));
    }

    // Guardar mientras una foto se comprime dejaría el slot con el archivo sin
    // comprimir, que es justo lo que rompe la subida. Se espera antes del POST.
    async function esperarCompresionGaleria() {
        while (procesandoGaleria.size) await Promise.all([...procesandoGaleria]);
    }

    // Vacía el slot. Si había foto guardada en ese orden, se detecta al guardar
    // (planGaleria lo ve como "existe y ya no hay archivo" -> DELETE).
    function quitarFotoGaleria(orden) {
        const previa = fotosGaleria.value[orden];
        if (previa) URL.revokeObjectURL(previa.preview);
        Object.assign(fotosGaleria.value, { [orden]: null });
    }

    // Abre el formulario en modo crear (vacío)
    function addEvent() {
        editingId.value = null;
        limpiarErrores();
        liberarPreviewsGaleria();
        inputEstablishment.value = "";
        inputAddress.value = "";
        inputDate.value = "";
        inputFechaFin.value = "";
        inputHoraInicio.value = "";
        inputHoraFin.value = "";
        inputNombre.value = "";
        inputDescripcion.value = "";
        inputRedes.value = [];
        inputTelefono.value = "";
        inputUrlEvento.value = "";
        inputEtiquetas.value = [];
        inputPoster.value = "";
        file.value = null;
        fileHd.value = null;
        inputMap.value = "";
        showForm.value = true;
        showPreview.value = false;
        previewEv.value = null;
        useEventosInvitado().showEvent.value = false;
        useEventosInvitado().showCover.value = false;
        useEventosInvitado().showNoEvent.value = false;
        useEventosInvitado().showProximos.value = false;
        useGestionEventos().showManage.value = false;
        useModeracion().showPendientes.value = false;
        useUsuarios().showUsuarios.value = false;
        useEtiquetas().showEtiquetas.value = false;
        usePerfil().showPerfil.value = false;
        usePanel().showPanel.value = false;
        useGestionEventos().showEventSaved.value = false;
    }

    function cancelForm() {
        editingId.value = null;
        showForm.value = false;
        showPreview.value = false;
        previewEv.value = null;
    }

    // US2 004: valida y muestra la previsualización sin enviar nada al backend
    function previsualizar() {
        if (!validarObligatorios()) return;
        if (inputMap.value && !inputMap.value.includes('google.com/maps/embed')) {
            alert('Por favor, ingresa un código de mapa válido de Google Maps');
            return;
        }
        if (inputFechaFin.value && inputDate.value && inputFechaFin.value < inputDate.value) {
            alert('La fecha de fin no puede ser anterior a la de inicio');
            return;
        }
        if (!cumple48h()) {
            alert('El evento debe programarse con al menos 48h de antelación');
            return;
        }
        previewEv.value = {
            id: null,
            establishment: inputEstablishment.value,
            address: inputAddress.value,
            date: inputDate.value,
            fechaFin: inputFechaFin.value || inputDate.value,
            // 019: la tarjeta muestra el nombre; la descripción va aparte.
            content: inputNombre.value,
            description: inputDescripcion.value,
            redes: inputRedes.value.map(r => ({ red: r.red, url: r.url })),
            telefono: inputTelefono.value || null,
            urlEvento: inputUrlEvento.value || null,
            etiquetas: [...inputEtiquetas.value], // vacío = OTROS (lo pone el backend)
            horaInicio: inputHoraInicio.value ? inputHoraInicio.value + ":00" : null,
            horaFin: inputHoraFin.value ? inputHoraFin.value + ":00" : null,
            poster: inputPoster.value,
            map: processMap(inputMap.value)
        };
        showForm.value = false;
        useEventosInvitado().showCover.value = false;
        showPreview.value = true;
    }

    function volverAEditar() {
        showPreview.value = false;
        showForm.value = true;
    }

    async function confirmarEnvio() {
        if (await submitForm()) {
            showPreview.value = false;
            previewEv.value = null;
            if (window.location.pathname.includes("organizador")) {
                await useMisEventos().loadMisEventos();
            }
        }
    }

    // 48h de antelación con la hora local del navegador (el backend revalida; esto es solo UX)
    function cumple48h() {
        if (!inputDate.value) return false;
        const [y, m, d] = inputDate.value.split('-').map(Number);
        const [hh, mm] = (inputHoraInicio.value || "00:00").split(':').map(Number);
        const inicio = new Date(y, m - 1, d, hh, mm);
        return inicio.getTime() - Date.now() >= 48 * 60 * 60 * 1000;
    }

    async function submitForm() {
        // 019: si el usuario pulsa guardar nada más elegir las imágenes, la compresión a
        // WebP puede seguir en curso. Se espera ANTES de validar, porque la validación del
        // cartel obligatorio mira file.value y llegaría anull con la imagen ya elegida.
        // Subir el original es justo lo que supera los 2MB del backend y rompe la subida;
        // en el caso del cartel, sin esta espera se perdía la foto en silencio.
        await esperarCompresionCartel();
        await esperarCompresionGaleria();

        if (!validarObligatorios()) return false;
        if (inputMap.value && !inputMap.value.includes('google.com/maps/embed')) {
            alert('Por favor, ingresa un código de mapa válido de Google Maps');
            return;
        }

        if (inputFechaFin.value && inputDate.value && inputFechaFin.value < inputDate.value) {
            alert('La fecha de fin no puede ser anterior a la de inicio');
            return;
        }

        const dto = {
            establecimiento: inputEstablishment.value,
            direccion: inputAddress.value,
            fecha: inputDate.value,
            // Vacía = un día (el backend la normaliza a fecha)
            fechaFin: inputFechaFin.value || null,
            nombre: inputNombre.value,
            descripcion: inputDescripcion.value,
            // 019 (F190): el evento lleva su propia lista de redes ({red, url}),
            // teléfono de contacto y URL pública. Vacío = el backend guarda sin ellos.
            redesSociales: inputRedes.value.map(r => ({ red: r.red, url: r.url })),
            telefonoEvento: inputTelefono.value || null,
            urlEvento: inputUrlEvento.value || null,
            // Vacío = ["OTROS"] (lo pone el backend por defecto)
            etiquetas: [...inputEtiquetas.value],
            // Vacío = null (sin horario); el backend exige ambas o ninguna
            horaInicio: inputHoraInicio.value || null,
            horaFin: inputHoraFin.value || null,
            cartelUrl: "",
            mapaEmbed: processMap(inputMap.value)
        };

        let r;
        try {
            r = await guardar({
                dto,
                file: file.value,
                fileHd: fileHd.value,
                editingId: editingId.value
            });
        } catch (e) {
            return false; // red/401 ya gestionados en api/http.js (alerta + redirección)
        }
        if (!r.ok) {
            alert(r.message);
            return false;
        }

        // El evento ya existe: sin id no hay contra qué sincronizar la galería
        // (el backend devuelve el guardado, y en editar ya lo teníamos).
        const id = (r.evento && r.evento.id) || editingId.value;
        const fallos = id ? await sincronizarGaleria(id) : [];
        // Aunque alguna foto falle, el evento se guardó: se avisa y se sigue, para no
        // dejar al organizador con la sensación de que se perdió el evento entero.
        if (fallos.length) alert(fallos.join("\n"));

        liberarPreviewsGaleria();
        inputEstablishment.value = "";
        inputAddress.value = "";
        inputDate.value = "";
        inputFechaFin.value = "";
        inputHoraInicio.value = "";
        inputHoraFin.value = "";
        inputNombre.value = "";
        inputDescripcion.value = "";
        inputRedes.value = [];
        inputTelefono.value = "";
        inputUrlEvento.value = "";
        inputEtiquetas.value = [];
        inputPoster.value = "";
        file.value = null;
        fileHd.value = null;
        inputMap.value = "";
        editingId.value = null;

        showForm.value = false;
        showPreview.value = false;
        previewEv.value = null;
        const gestion = useGestionEventos();
        gestion.showEventSaved.value = true;
        await gestion.loadAllEvents();
        const inv = useEventosInvitado();
        if (inv.searchDate.value) await inv.searchEvent();
        return true;
    }

    // 019: aplica lo que planGaleria decidió. Cada foto va en su propia petición
    // (el backend no acepta lote) y un fallo se acumula para avisar de una vez.
    async function sincronizarGaleria(id) {
        const fallos = [];
        for (const { orden, accion } of planGaleria(galeriaSlots, fotosGaleria.value, galeriaExistente.value)) {
            if (accion === "subir") {
                const r = await subirFotoGaleria(id, orden, fotosGaleria.value[orden].file);
                if (!r.ok) fallos.push(r.message);
            } else if (!await borrarFotoGaleria(id, orden)) {
                fallos.push(`No se pudo quitar la foto ${orden + 1} del evento.`);
            }
        }
        return fallos;
    }

    // Precarga el formulario con el evento y entra en modo edición.
    // 019: es async porque hay que pedir los órdenes con foto para pintar los slots.
    async function editEvento(ev) {
        editingId.value = ev.id;
        limpiarErrores();
        liberarPreviewsGaleria();
        inputEstablishment.value = ev.establishment;
        inputAddress.value = ev.address;
        inputDate.value = ev.date;
        // fechaFin null en filas antiguas = un día
        inputFechaFin.value = (ev.fechaFin && ev.fechaFin !== ev.date) ? ev.fechaFin : "";
        // La API devuelve "HH:mm:ss"; el input time usa "HH:mm"
        inputHoraInicio.value = ev.horaInicio ? ev.horaInicio.slice(0, 5) : "";
        inputHoraFin.value = ev.horaFin ? ev.horaFin.slice(0, 5) : "";
        // 019: `content` es el nombre; el nombre aún puede no venir separado.
        inputNombre.value = ev.nombre || ev.content || "";
        inputDescripcion.value = ev.description || "";
        inputTelefono.value = ev.telefono || "";
        inputUrlEvento.value = ev.urlEvento || "";
        inputRedes.value = (ev.redes || []).map(r => ({ red: r.red, url: r.url }));
        inputEtiquetas.value = [...(ev.etiquetas || [])];
        inputPoster.value = ev.poster;
        file.value = null;
        fileHd.value = null;
        galeriaExistente.value = await listarIndicesGaleria(ev.id);
        inputMap.value = ev.map; // iframe actual precargado (vaciarlo lo borra)
        showForm.value = true;
        useGestionEventos().showManage.value = false;
        useUsuarios().showUsuarios.value = false;
        useEtiquetas().showEtiquetas.value = false;
        usePerfil().showPerfil.value = false;
        usePanel().showPanel.value = false;
        const inv = useEventosInvitado();
        inv.showEvent.value = false;
        inv.showCover.value = false;
        inv.showNoEvent.value = false;
        inv.showProximos.value = false;
        useGestionEventos().showEventSaved.value = false;
    }

    instance = {
        editingId,
        inputEstablishment, inputAddress, inputDate, inputFechaFin,
        inputHoraInicio, inputHoraFin, inputNombre, inputDescripcion,
        inputRedes, inputTelefono, inputUrlEvento, añadirRed, quitarRed,
        inputEtiquetas,
        inputPoster, inputMap, file, fileHd,
        // 019: slots de galería
        galeriaSlots, fotosGaleria, galeriaExistente,
        slotOcupado, slotPendienteBorrar, slotPreview, elegirFotoGaleria, quitarFotoGaleria,
        showForm, showPreview, previewEv,
        errEstablishment, errAddress, errDate, errNombre, errCartel,
        addEvent, cancelForm, previsualizar, volverAEditar, confirmarEnvio,
        cumple48h, submitForm, editEvento, validarObligatorios
    };
    return instance;
}
