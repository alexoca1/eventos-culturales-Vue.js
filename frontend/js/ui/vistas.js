// Mapa de vistas de la aplicación.
// Datos puros, sin dependencias del navegador: el composable useVista.js los
// usa para decidir qué sección está visible y cuál se puede reabrir, y
// vistas.check.js los verifica con node (sin framework).

// Secciones reabribles por URL (?vista=<nombre>), EN ORDEN DE PRECEDENCIA:
// si por un fallo de estado varias estuvieran a la vez, gana la primera.
// Los estados transitorios (previsualización del organizador, banner de
// "evento guardado") NO se listan a propósito: al recargar no se restauran y
// la URL conserva la última sección real desde la que se llegó.
export const VISTAS = [
    "pendientes",   // admin: cola de moderación
    "usuarios",     // admin: gestión de usuarios
    "etiquetas",    // admin: CRUD de etiquetas
    "gestionar",    // admin: lista completa para editar/eliminar
    "favoritos",    // usuario estándar: mis favoritos
    "perfil",       // los tres roles: editar mis datos
    "panel",        // los tres roles: "Mi panel" con contadores
    "formulario",   // crear/editar evento
    "mis-eventos",  // organizador: sus eventos
    "proximos",     // cartelera pública (próximos)
    "busqueda",     // resultados de buscar por fecha/etiquetas
    "sin-resultados", // la búsqueda no encontró nada
    "portada"       // imagen de portada (sin sección listada)
];

// Vista inicial de cada página de rol. Es la que se ve al llegar y la que
// restaura la cabecera ("irAInicio"). El invitado aterriza en la cartelera
// porque es lo que ya carga invitado.js; el organizador en "mis eventos".
export const LANDING = {
    administrador: "portada",
    organizador: "mis-eventos",
    usuarioEstandar: "proximos"
};

// Página de panel que corresponde a cada rol (ruta relativa desde views/).
export const PANEL_POR_ROL = {
    ROLE_ADMIN: "administrador.html",
    ROLE_ORGANIZADOR: "organizador.html"
};
