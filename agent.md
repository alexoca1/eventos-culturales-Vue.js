# Agent.md - Eventos Culturales Puertollano

## Descripción del Proyecto

Aplicación web para consultar y gestionar eventos culturales en la localidad de Puertollano (Ciudad Real, España). Permite a los usuarios invitados buscar eventos por fecha y a los administradores realizar operaciones CRUD sobre los eventos.

## Tecnologías

- **Frontend**: HTML5, CSS3, JavaScript
- **Framework**: Vue.js 3 (CDN - `lib/vue.global.js`)
- **Sin backend**: Los datos se almacenan en memoria (array en `js/eventos.js`)
- **APIs externas**: Google Maps (iframes embebidos)

## Estructura del Proyecto

```
├── index.html              # Página principal (acceso invitado/admin)
├── views/
│   ├── usuarioEstandar.html  # Vista del invitado (búsqueda por fecha)
│   └── administrador.html    # Vista del admin (CRUD eventos)
├── js/
│   └── eventos.js          # Lógica principal (Vue.js app)
├── css/
│   ├── index.css           # Estilos de la página principal
│   └── eventos.css         # Estilos de las vistas
├── lib/
│   └── vue.global.js       # Vue.js 3
└── img/                    # Imágenes del proyecto
```

## Autenticación

- **Administrador**: 
  - Usuario: `alexander`
  - Contraseña: `***REMOVED-CREDENTIAL***`
- **Invitado**: Acceso directo sin credenciales

## Modelo de Datos (Evento)

```javascript
{
    establishment: string,  // Nombre del establecimiento
    address: string,       // Dirección completa
    date: string,          // Fecha en formato YYYY-MM-DD
    content: string,       // Nombre/descripción del evento
    poster: string,        // HTML de imagen del cartel
    map: string            // HTML iframe de Google Maps
}
```

## Funcionalidades

### Usuario Invitado (`views/usuarioEstandar.html`)
- Buscar eventos por fecha usando el selector de fecha
- Visualizar detalles del evento: cartel, fecha, establecimiento, dirección, evento principal, ubicación en mapa
- Mostrar mensaje cuando no hay eventos para la fecha seleccionada

### Administrador (`views/administrador.html`)
- **Buscar**: Consultar eventos por fecha
- **Crear**: Formulario para agregar nuevos eventos (fecha, establecimiento, dirección, contenido, cartel, mapa)
- **Eliminar**: Seleccionar uno o más eventos para eliminar
- Las imágenes se redimensionan automáticamente a max 400x400px

## Métodos Principales (`js/eventos.js`)

| Método | Descripción |
|--------|-------------|
| `validateAdmin()` | Valida credenciales del administrador |
| `searchEvent()` | Busca eventos por fecha |
| `addEvent()` | Muestra formulario de nuevo evento |
| `submitForm()` | Guarda el nuevo evento |
| `deleteEvent()` | Muestra panel de eliminación |
| `deleteSelectedEvents()` | Elimina eventos seleccionados |
| `formatDate(date)` | Convierte fecha YYYY-MM-DD a DD/MM/AA |
| `processMap(iframe)` | Procesa y valida iframe de Google Maps |
| `handleFileUpload(event)` | Maneja carga de imagen del cartel |

## Datos de Ejemplo

El proyecto incluye 5 eventos de ejemplo en Puertollano:
1. El Mesoncito - Fiesta Mexicana (01/09/2026)
2. Restaurante HAVANA - Monólogo Danni Robira (01/10/2026)
3. Restaurante NAKAMA - Exhibición de tapas vegetarianas (01/11/2026)
4. Auditorio Municipal - Rock la Mancha Festival (01/12/2026)
5. Museo Cristina García Rodero - Exposición de Arte (01/01/2027)

## Notas para Desarrollo

- Los datos NO persisten (se pierden al recargar la página)
- La autenticación es básica (hardcodeada, sin hashing)
- El código iframe de Google Maps se valida que contenga `google.com/maps/embed`
- Las imágenes se procesan client-side con FileReader
- Los archivos CSS están separados: `index.css` (página principal) y `eventos.css` (vistas internas)
